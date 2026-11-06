package com.nexus.app.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexus.app.nexus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try { block() } finally { pending.finish() }
    }
}

/** Alarm (session due / pause expired) and notification actions. */
class FocusEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val focus = context.nexus.focus
        runAsync {
            when (intent.action) {
                ACTION_PAUSE -> focus.pause()
                ACTION_RESUME -> focus.resume()
                else -> focus.reconcileAndSync(fromBackground = true)
            }
        }
    }

    companion object {
        const val ACTION_DUE = "com.nexus.focus.DUE"
        const val ACTION_PAUSE = "com.nexus.focus.PAUSE"
        const val ACTION_RESUME = "com.nexus.focus.RESUME"
    }
}

/** Device restart / app update: alarms were cleared, re-derive everything from the database. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        runAsync { context.nexus.focus.reconcileAndSync(fromBackground = true) }
    }
}
