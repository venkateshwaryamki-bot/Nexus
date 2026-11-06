package com.nexus.app.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.nexus.domain.time.Clock

class AndroidClock(private val context: Context) : Clock {
    override fun wallMillis() = System.currentTimeMillis()
    override fun elapsedMillis() = SystemClock.elapsedRealtime()
    override fun bootId(): String {
        val count = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull()
        // Fallback: boot instant derived from both clocks, rounded to absorb jitter.
        return count?.toString() ?: "t" + ((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 60_000)
    }
}
