package com.nexus.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.nexus.app.NexusViewModel
import com.nexus.app.Route
import com.nexus.app.ui.components.*
import com.nexus.app.ui.theme.NexusColors
import com.nexus.app.ui.theme.NexusType

/**
 * Missions / Planets / Rankings are Milestone 2-4 features. Rather than fake them, the dock leads to
 * an in-universe "sector offline" panel that states plainly what is coming.
 */
@Composable
fun LockedSectorScreen(route: Route, vm: NexusViewModel, onBack: () -> Unit) {
    val (title, body) = when (route) {
        Route.MISSIONS -> "Missions" to "Daily, weekly and special missions with rewards. Arrives in the next update (Milestone 2). Sessions you complete now are already recorded."
        Route.BUILD -> "Build" to "Construct and upgrade Energy Reactors, Research Labs, Spaceports and more with your energy. Each upgrade visibly changes Nexus Prime. Arrives in the planet development update (Phase 5)."
        Route.PLANETS -> "Galaxy map" to "Explore Orion, Aether, Titan and Eclipse. Each world unlocks through your focus progress. The galaxy map arrives in Milestone 2; the worlds themselves follow in Milestone 3."
        else -> "Rankings" to "Friends, weekly and global rankings need NEXUS online services, which arrive in Milestone 4. NEXUS will never present simulated players as real rankings."
    }
    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            ScreenHeader(title, "Sector offline", onBack, accent = NexusColors.Purple)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (route == Route.PLANETS) {
                    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                        val n = PlanetIdentity.entries.size
                        PlanetIdentity.entries.forEachIndexed { i, p -> drawPlanetOrb(p, Offset(size.width * (i + 0.5f) / n, size.height / 2 + if (i % 2 == 0) -14f else 14f), size.height * 0.22f, i != 0) }
                    }
                } else CharacterPortrait(Character.NEXA, size = 200.dp)
                Spacer(Modifier.height(16.dp))
                GlassPanel(Modifier.fillMaxWidth(), accent = NexusColors.Purple) {
                    SectionLabel("NEXA · Nexus Intelligence", color = NexusColors.Purple)
                    Spacer(Modifier.height(8.dp))
                    Text(body, style = NexusType.Body, color = NexusColors.TextPrimary)
                }
            }
            Spacer(Modifier.weight(1.3f))
        }
    }
}
