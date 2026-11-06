package com.nexus.app.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * One alarm at a time, on the monotonic ELAPSED_REALTIME clock (immune to wall-clock changes).
 * Uses an exact alarm when the user allowed it; otherwise an inexact allow-while-idle alarm, which
 * Android may defer by several minutes in Doze. Either way, rewards are computed from timestamps,
 * so a late alarm only delays the *notification*, never the accuracy of the session.
 */
class FocusAlarmScheduler(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    private fun intent() = PendingIntent.getBroadcast(
        context, 1, Intent(context, FocusEventReceiver::class.java).setAction(FocusEventReceiver.ACTION_DUE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun scheduleIn(delayMs: Long) {
        val at = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(0) + 500
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent())
            else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent())
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent())
        }
    }

    fun cancel() = am.cancel(intent())
}
