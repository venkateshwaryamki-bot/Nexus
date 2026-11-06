package com.nexus.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.data.PlayerSnapshot
import com.nexus.data.ProfileStats
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.ReconcileResult
import com.nexus.world.scene.GraphicsQuality
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppSettings(
    val audio: Boolean = true,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val quality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val defaultMinutes: Int = 25,
    val dailyGoalSessions: Int = 2,
    /** Home planet rendering: false = illustrated 2.5D (default), true = live 3D (GLES WorldHost). */
    val liveWorld: Boolean = false,
    /** Ambient soundscape in the Focus Chamber. */
    val ambientSound: Boolean = false,
) {
    companion object {
        fun from(m: Map<String, String>) = AppSettings(
            audio = m["audio"]?.toBoolean() ?: true,
            haptics = m["haptics"]?.toBoolean() ?: true,
            reduceMotion = m["reduce_motion"]?.toBoolean() ?: false,
            quality = m["quality"]?.let { runCatching { GraphicsQuality.valueOf(it) }.getOrNull() } ?: GraphicsQuality.MEDIUM,
            defaultMinutes = m["default_minutes"]?.toIntOrNull() ?: 25,
            dailyGoalSessions = m["daily_goal"]?.toIntOrNull() ?: 2,
            liveWorld = m["live_world"]?.toBoolean() ?: false,
            ambientSound = m["ambient_sound"]?.toBoolean() ?: false,
        )
    }
}

/**
 * Outcome shown on the result screen. [after] is read from the database right after the settlement
 * committed; "before" values are derived by subtracting the reward, so nothing races the DB flows.
 */
data class SessionOutcome(val result: ReconcileResult.Closed, val after: PlayerSnapshot?)

class NexusViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.nexus

    val player: StateFlow<PlayerSnapshot?> = c.players.observePlayer().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val stats: StateFlow<ProfileStats?> = c.players.observeStats { c.coordinator.today() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val settings: StateFlow<AppSettings> = c.settings.observe().map(AppSettings::from)
        .onEach { c.audio.enabled = it.audio }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val openSession: StateFlow<FocusSession?> = c.focus.observeOpenSession().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** Distinct recent task names, newest first (task picker suggestions). */
    val recentTasks: StateFlow<List<String>> = c.sessions.observeRecentSessions(30)
        .map { l -> l.map { it.task.trim() }.filter { it.isNotEmpty() }.distinct().take(4) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val buildingLevels: StateFlow<Map<com.nexus.domain.civilization.BuildingType, Int>> =
        c.buildings.observeLevels().stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    private val _lastBuild = MutableStateFlow<com.nexus.domain.repository.BuildResult?>(null)
    val lastBuild: StateFlow<com.nexus.domain.repository.BuildResult?> = _lastBuild
    fun build(type: com.nexus.domain.civilization.BuildingType, expectedLevel: Int) = viewModelScope.launch {
        val r = c.buildings.build(type, expectedLevel)
        if (r is com.nexus.domain.repository.BuildResult.Built) c.audio.play(com.nexus.app.platform.AudioController.Sfx.ENERGY)
        if (r !is com.nexus.domain.repository.BuildResult.Stale) _lastBuild.value = r
    }
    fun consumeBuild() { _lastBuild.value = null }
    val loaded: StateFlow<Boolean> = player.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _outcome = MutableStateFlow<SessionOutcome?>(null)
    val outcome: StateFlow<SessionOutcome?> = _outcome
    /** Energy pulse waiting to be played on the home planet. */
    private val _pendingPulse = MutableStateFlow(0)
    val pendingPulse: StateFlow<Int> = _pendingPulse
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val engine get() = c.focus.engine
    val clock get() = c.clock
    val audio get() = c.audio
    val platform get() = c.platform

    init {
        viewModelScope.launch {
            c.focus.unseenResult.filterNotNull().collect { r ->
                _outcome.value = SessionOutcome(r, c.players.snapshot())
                if (r.reward.energy > 0) _pendingPulse.value = r.reward.energy
                c.audio.play(com.nexus.app.platform.AudioController.Sfx.COMPLETE)
                c.focus.acknowledgeResult()
            }
        }
    }

    fun completeOnboarding(name: String, avatar: Int) = viewModelScope.launch { c.players.completeOnboarding(name, avatar) }
    fun putSetting(key: String, value: Any) = viewModelScope.launch { c.settings.put(key, value.toString()) }
    fun resetProgress() = viewModelScope.launch {
        c.focus.endEarly(); c.settings.resetProgress(); c.players.ensureInitialized(); _outcome.value = null
    }

    fun startSession(task: String, minutes: Int, envId: String) = viewModelScope.launch {
        runCatching { c.focus.start(task, minutes, envId) }.onFailure { _error.value = it.message }
    }
    fun pause() = viewModelScope.launch { c.focus.pause() }
    fun resume() = viewModelScope.launch { c.focus.resume() }
    fun endEarly() = viewModelScope.launch { c.focus.endEarly() }
    fun reconcile() = viewModelScope.launch { c.focus.reconcileAndSync() }
    fun clearError() { _error.value = null }

    fun consumeOutcome() { _outcome.value = null }
    fun consumePulse(): Int = _pendingPulse.value.also { _pendingPulse.value = 0 }
}
