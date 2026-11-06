package com.nexus.app.platform

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexus.app.R

/**
 * Plays the looping ambient focus soundscape while [enabled] and the screen is visible.
 * It stops when the app goes to the background (the timer keeps running regardless).
 */
@Composable
fun AmbientSoundEffect(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(enabled, lifecycle) {
        var player: MediaPlayer? = null
        fun start() {
            if (!enabled || player != null) return
            player = runCatching {
                MediaPlayer.create(context, R.raw.ambient_focus)?.apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    isLooping = true; setVolume(0.55f, 0.55f); start()
                }
            }.getOrNull()
        }
        fun stop() { player?.runCatching { stop(); release() }; player = null }
        val obs = LifecycleEventObserver { _, e -> when (e) { Lifecycle.Event.ON_START -> start(); Lifecycle.Event.ON_STOP -> stop(); else -> {} } }
        lifecycle.addObserver(obs)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { lifecycle.removeObserver(obs); stop() }
    }
}
