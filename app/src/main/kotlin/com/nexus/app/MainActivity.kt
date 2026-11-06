package com.nexus.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.ui.screens.*
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusPrefs
import com.nexus.app.ui.theme.NexusTheme

enum class Route { INTRO, ONBOARDING, CHARACTERS, HOME, FOCUS, RESULT, PROFILE, SETTINGS, MISSIONS, PLANETS, RANKINGS }

class MainActivity : ComponentActivity() {
    private val vm: NexusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            NexusTheme {
                CompositionLocalProvider(LocalNexusPrefs provides NexusPrefs(settings.reduceMotion, settings.haptics)) {
                    Box(Modifier.fillMaxSize().background(NexusColors.DeepSpaceNavy)) { NexusApp(vm) }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); vm.reconcile() }
}

@Composable
private fun NexusApp(vm: NexusViewModel) {
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val open by vm.openSession.collectAsStateWithLifecycle()
    if (!loaded) return
    var route by rememberSaveable { mutableStateOf(if (player?.onboardingComplete == true) (if (open != null) Route.FOCUS else Route.HOME) else Route.INTRO) }

    // A settled session (from any source) takes the player to the debrief.
    LaunchedEffect(outcome) { if (outcome != null) route = Route.RESULT }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val go: (Route) -> Unit = { route = it }
    BackHandler(enabled = route !in setOf(Route.HOME, Route.INTRO, Route.ONBOARDING)) {
        route = when (route) { Route.SETTINGS -> Route.PROFILE; Route.CHARACTERS -> Route.ONBOARDING; else -> Route.HOME }
    }

    AnimatedContent(route, transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) }, label = "route") { r ->
        when (r) {
            Route.INTRO -> IntroScreen(vm, onDone = { go(if (player?.onboardingComplete == true) Route.HOME else Route.ONBOARDING) })
            Route.ONBOARDING -> OnboardingScreen(onComplete = { name, avatar -> vm.completeOnboarding(name, avatar); go(Route.CHARACTERS) })
            Route.CHARACTERS -> CharacterIntroScreen(onDone = { go(Route.HOME) })
            Route.HOME -> HomeScreen(vm, go)
            Route.FOCUS -> FocusScreen(vm, onBack = { go(Route.HOME) }, requestNotifications = {
                if (Build.VERSION.SDK_INT >= 33 && !vm.platform.canPostNotifications()) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
            Route.RESULT -> ResultScreen(vm, onContinue = { vm.consumeOutcome(); go(Route.HOME) })
            Route.PROFILE -> ProfileScreen(vm, onBack = { go(Route.HOME) }, onSettings = { go(Route.SETTINGS) })
            Route.SETTINGS -> SettingsScreen(vm, onBack = { go(Route.PROFILE) }, onReplayIntro = { go(Route.INTRO) })
            Route.MISSIONS, Route.PLANETS, Route.RANKINGS -> LockedSectorScreen(r, vm, onBack = { go(Route.HOME) })
        }
    }
}
