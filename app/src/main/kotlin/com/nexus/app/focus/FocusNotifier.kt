package com.nexus.app.focus

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexus.app.MainActivity
import com.nexus.app.R
import com.nexus.domain.focus.FocusSession
import com.nexus.domain.focus.ReconcileResult
import com.nexus.domain.focus.SessionState
import com.nexus.domain.reward.RewardKind

/**
 * Notifications without a foreground service: the ongoing notification uses the system
 * chronometer (countdown), so Android renders the live timer itself and NEXUS needs no running
 * process during a session. This is what keeps long sessions battery-cheap.
 */
class FocusNotifier(private val context: Context) {
    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val m = context.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel(CH_SESSION, context.getString(R.string.channel_session), NotificationManager.IMPORTANCE_LOW)
            .apply { description = context.getString(R.string.channel_session_desc); setShowBadge(false) })
        m.createNotificationChannel(NotificationChannel(CH_RESULT, context.getString(R.string.channel_complete), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = context.getString(R.string.channel_complete_desc) })
    }

    private fun openApp() = PendingIntent.getActivity(context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun action(a: String, code: Int) = PendingIntent.getBroadcast(context, code,
        Intent(context, FocusEventReceiver::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun base(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_nexus)
        .setColor(0xFF27DFFF.toInt())
        .setContentIntent(openApp())

    private fun title(s: FocusSession) = s.task.ifBlank { "Focus session" }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    private fun post(id: Int, n: android.app.Notification) { if (nm.areNotificationsEnabled()) nm.notify(id, n) }

    fun showRunning(s: FocusSession, remainingMs: Long, canPause: Boolean) {
        val b = base(CH_SESSION)
            .setContentTitle(title(s))
            .setContentText("Focus in progress · ${s.plannedMinutes} min session · +${s.plannedMinutes} energy on completion")
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setUsesChronometer(true).setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remainingMs).setShowWhen(true)
        if (canPause) b.addAction(0, "Pause", action(FocusEventReceiver.ACTION_PAUSE, 2))
        post(ID_SESSION, b.build())
    }

    fun showPaused(s: FocusSession, remainingMs: Long, pauseLeftMs: Long) {
        val mm = remainingMs / 60_000; val ss = (remainingMs / 1000) % 60
        post(ID_SESSION, base(CH_SESSION)
            .setContentTitle("Paused · ${title(s)}")
            .setContentText("%d:%02d left. Resume within %d min or the session ends.".format(mm, ss, (pauseLeftMs + 59_999) / 60_000))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setUsesChronometer(true).setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + pauseLeftMs)
            .addAction(0, "Resume", action(FocusEventReceiver.ACTION_RESUME, 3))
            .build())
    }

    fun showResult(r: ReconcileResult.Closed) {
        val completed = r.session.state == SessionState.COMPLETED
        val text = when {
            completed -> "+${r.reward.energy} energy · +${r.reward.xp} XP. Nexus Prime grows stronger."
            r.reward.kind == RewardKind.PARTIAL -> "Session ended early · +${r.reward.energy} energy (partial)."
            else -> "Session ended before the 10-minute minimum. No energy this time."
        }
        post(ID_RESULT, base(CH_RESULT)
            .setContentTitle(if (completed) "Session complete" else "Session ended")
            .setContentText(text).setAutoCancel(true).build())
    }

    fun cancelOngoing() = nm.cancel(ID_SESSION)

    companion object {
        const val CH_SESSION = "focus_session"; const val CH_RESULT = "focus_result"
        const val ID_SESSION = 1001; const val ID_RESULT = 1002
    }
}
