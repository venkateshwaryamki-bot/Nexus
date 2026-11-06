package com.nexus.app

import android.content.Context
import android.view.View
import com.nexus.world.gles.NexusWorldView
import com.nexus.app.focus.FocusAlarmScheduler
import com.nexus.app.focus.FocusNotifier
import com.nexus.app.focus.FocusSystem
import com.nexus.app.platform.AndroidClock
import com.nexus.app.platform.AudioController
import com.nexus.app.platform.PlatformStatus
import com.nexus.data.NexusDatabase
import com.nexus.data.PlayerRepository
import com.nexus.data.RoomProgressionRepository
import com.nexus.data.RoomSessionRepository
import com.nexus.data.SettingsRepository
import com.nexus.domain.focus.FocusCoordinator
import com.nexus.domain.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container. The app is small enough that a DI framework would add build time and
 * APK size without benefit (see TECHNICAL_DECISIONS.md, "Avoided dependencies").
 */
class AppContainer(
    context: Context,
    val clock: Clock = AndroidClock(context),
    /**
     * Creates the 3D world view. The app depends only on [com.nexus.world.scene.WorldHost]; the default
     * is the native GLES host. Swapping engines (or a fake in tests) means providing another factory.
     */
    val worldHostFactory: (Context) -> View = { NexusWorldView(it) },
) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = NexusDatabase.create(context)
    val sessions = RoomSessionRepository(db)
    val progression = RoomProgressionRepository(db)
    val players = PlayerRepository(db)
    val settings = SettingsRepository(db)
    val buildings = com.nexus.data.RoomBuildingRepository(db)
    val coordinator = FocusCoordinator(sessions, progression, clock)
    val platform = PlatformStatus(context)
    val notifier = FocusNotifier(context)
    val alarms = FocusAlarmScheduler(context)
    val focus = FocusSystem(coordinator, sessions, alarms, notifier, clock, platform)
    val audio = AudioController(context)
}
