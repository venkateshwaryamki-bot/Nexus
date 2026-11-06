package com.nexus.app.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.nexus.app.R

/** Short UI/cinematic effects via SoundPool (low latency, tiny memory footprint). */
class AudioController(context: Context) {
    enum class Sfx { INTRO, COMPLETE, ENERGY }
    @Volatile var enabled = true
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val ready = mutableSetOf<Int>()
    private val pendingPlay = mutableSetOf<Int>()
    private val ids = mapOf(
        Sfx.INTRO to pool.load(context, R.raw.nexus_intro, 1),
        Sfx.COMPLETE to pool.load(context, R.raw.session_complete, 1),
        Sfx.ENERGY to pool.load(context, R.raw.energy_gain, 1),
    )

    init {
        pool.setOnLoadCompleteListener { p, id, status ->
            synchronized(this) {
                if (status == 0) ready += id
                if (id in pendingPlay) { pendingPlay -= id; if (enabled) p.play(id, 0.9f, 0.9f, 1, 0, 1f) }
            }
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val id = ids.getValue(sfx)
        synchronized(this) { if (id in ready) pool.play(id, 0.9f, 0.9f, 1, 0, 1f) else pendingPlay += id }
    }
}
