package com.nexus.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nexus.app.focus.FocusSystem
import kotlinx.coroutines.launch

open class NexusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        container.notifier.createChannels()
        container.appScope.launch { container.players.ensureInitialized(); container.focus.reconcileAndSync() }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.appScope.launch { container.focus.reconcileAndSync() }
            }
            override fun onStop(owner: LifecycleOwner) {
                // App left the foreground. Turning the screen off is NOT a distraction; switching apps is.
                if (container.platform.isScreenInteractive()) {
                    container.appScope.launch { container.focus.recordInterruption() }
                }
            }
        })
    }

    /** Overridable for tests (e.g. a controllable clock). */
    protected open fun createContainer() = AppContainer(this)
}

val android.content.Context.nexus: AppContainer get() = (applicationContext as NexusApplication).container
