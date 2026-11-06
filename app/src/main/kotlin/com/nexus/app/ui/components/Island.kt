package com.nexus.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nexus.app.ui.art.IslandLayer
import com.nexus.app.ui.art.IslandLayout
import com.nexus.app.ui.theme.LocalNexusPrefs
import com.nexus.app.ui.theme.NexusColors
import com.nexus.domain.civilization.BuildingType
import com.nexus.domain.civilization.GroveTree
import com.nexus.domain.civilization.TreeStage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun BuildingType.layerKey(level: Int) = "b_${name.lowercase()}_$level"

/** Layer names to stack for a civilization stage and the player's building levels (back-to-front order is by depth). */
fun islandLayers(stage: Int, levels: Map<BuildingType, Int>): List<Pair<String, IslandLayer>> {
    val s = stage.coerceIn(0, 4)
    val names = listOf("base_s$s", "tower_s$s") + levels.filterValues { it > 0 }.map { (t, l) -> t.layerKey(l.coerceAtMost(BuildingType.MAX_LEVEL)) }
    return names.mapNotNull { n -> IslandLayout.layers[n]?.let { n to it } }.sortedByDescending { it.second.depth }
}

/**
 * The living headquarters, composited from pre-rendered 2.5D layers (tools/art/island_layers.py):
 * terrain + tower for the civilization stage, plus every structure the player has built.
 *
 * Extras: idle float, breathing aura, energy motes, reward shockwave ([pulseKey]), construction
 * animation for [justBuilt], and a pulsing "ghost" preview of [ghost] (Build screen).
 */
@Composable
fun NexusIsland(
    stage: Int, levels: Map<BuildingType, Int>, stageTitle: String, modifier: Modifier = Modifier,
    pulseKey: Int = 0, justBuilt: Pair<BuildingType, Int>? = null, ghost: Pair<BuildingType, Int>? = null, animate: Boolean = true,
    grove: List<GroveTree> = emptyList(),
) {
    val reduce = LocalNexusPrefs.current.reduceMotion || !animate
    val time = rememberSceneTime()
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) { if (pulseKey > 0) { pulse.snapTo(0f); pulse.animateTo(1f, tween(2200, easing = LinearOutSlowInEasing)) } }
    val build = remember { Animatable(1f) }
    LaunchedEffect(justBuilt) { if (justBuilt != null) { build.snapTo(0f); build.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) } }
    val st = stage.coerceIn(0, 4)
    val built = levels.filterValues { it > 0 }
    val layers = remember(st, built) { islandLayers(st, built) }
    // Trees and structural layers share one depth order (painter's algorithm).
    val items: List<Pair<Float, Any>> = remember(layers, grove) {
        (layers.map { it.second.depth to (it as Any) } +
            grove.mapNotNull { t -> IslandLayout.treeSlots.getOrNull(t.index)?.let { it.depth to (t as Any) } })
            .sortedByDescending { it.first }
    }
    // Remember each tree's last stage so a stage change plays a growth pop.
    val grownAt = remember { mutableStateMapOf<Int, TreeStage>() }
    val auraStrength = 0.10f + 0.06f * st

    fun plural(n: Int, w: String) = "$n $w" + if (n == 1) "" else "s"
    Box(modifier.semantics { contentDescription = "Nexus Prime, $stageTitle" + (if (built.isNotEmpty()) ", ${plural(built.size, "structure")}" else "") +
        (if (grove.isNotEmpty()) ", ${plural(grove.size, "tree")}" else "") }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = time.value; val c = Offset(size.width / 2, size.height * 0.42f)
            val breathe = if (reduce) 1f else 1f + 0.05f * sin(t * 1.1f)
            val p = pulse.value; val flash = if (p < 1f) (1f - p) * 0.35f else 0f
            val rad = size.minDimension * 0.55f * breathe
            drawCircle(Brush.radialGradient(listOf(NexusColors.ElectricCyan.copy(alpha = auraStrength + flash), NexusColors.Purple.copy(alpha = auraStrength * 0.4f), Color.Transparent), c, rad), rad, c)
            if (st > 0 && !reduce) {
                for (i in 0 until 10 + st * 6) {
                    val a = i * 2.399f + t * (0.08f + (i % 5) * 0.02f); val r = size.minDimension * (0.32f + (i % 7) * 0.025f)
                    val y = c.y + sin(a) * r * 0.32f - ((t * 0.04f + i * 0.13f) % 1f) * size.height * 0.12f
                    drawCircle((if (i % 4 == 0) NexusColors.NeonGreen else NexusColors.ElectricCyan).copy(alpha = 0.25f + 0.5f * (0.5f + 0.5f * sin(t * 2f + i))),
                        (1.5f + (i % 3)) * density, Offset(c.x + cos(a) * r, y))
                }
            }
            if (p < 1f) {
                val w = size.minDimension * (0.15f + p * 0.75f)
                drawOval(NexusColors.NeonGreen.copy(alpha = (1f - p) * 0.9f), Offset(c.x - w, c.y + size.height * 0.02f - w * 0.32f), Size(w * 2, w * 0.64f), style = Stroke((1f - p) * 8f * density))
            }
        }
        BoxWithConstraints(Modifier.aspectRatio(IslandLayout.ASPECT, matchHeightConstraintsFirst = true).graphicsLayer {
            val t = time.value
            translationY = if (reduce) 0f else sin(t * 0.9f) * 6f * density
            rotationZ = if (reduce) 0f else sin(t * 0.45f) * 0.6f
            val p = pulse.value; val pop = if (p < 1f) sin(p * PI.toFloat()) * 0.025f else 0f
            scaleX = 1f + pop; scaleY = 1f + pop
        }) {
            val fw = maxWidth; val fh = maxHeight
            items.forEach { (_, item) ->
                if (item is GroveTree) { key("tree${item.index}") { GroveTreeSprite(item, fw, fh, time, reduce, grownAt) }; return@forEach }
                @Suppress("UNCHECKED_CAST") val pair = item as Pair<String, IslandLayer>
                val (name, l) = pair
                val isNew = justBuilt != null && name == justBuilt.first.layerKey(justBuilt.second)
                key(name) {
                    Image(painterResource(l.res), null, contentScale = ContentScale.FillBounds, modifier = Modifier
                        .offset(fw * l.x, fh * l.y).size(fw * l.w, fh * l.h)
                        .graphicsLayer {
                            if (isNew) {
                                val b = build.value
                                val s = 0.55f + 0.45f * FastOutSlowInEasing.transform(b) + sin(b * PI.toFloat()) * 0.08f
                                scaleX = s; scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.92f)
                                alpha = b.coerceIn(0f, 1f)
                            }
                        })
                }
            }
            // Ghost preview of the next construction (Build screen).
            ghost?.let { (type, level) ->
                IslandLayout.layers[type.layerKey(level)]?.let { l ->
                    Image(painterResource(l.res), null, contentScale = ContentScale.FillBounds, modifier = Modifier
                        .offset(fw * l.x, fh * l.y).size(fw * l.w, fh * l.h)
                        .graphicsLayer { alpha = if (reduce) 0.45f else 0.3f + 0.25f * (0.5f + 0.5f * sin(time.value * 3f)) })
                }
            }
            // Construction sparks rising from the new structure.
            if (justBuilt != null && build.value < 1f) {
                IslandLayout.layers[justBuilt.first.layerKey(justBuilt.second)]?.let { l ->
                    Canvas(Modifier.fillMaxSize()) {
                        val b = build.value
                        val cx = size.width * (l.x + l.w / 2); val base = size.height * (l.y + l.h * 0.9f)
                        for (i in 0 until 18) {
                            val a = i * 0.349f; val r = size.width * l.w * (0.2f + 0.5f * b)
                            drawCircle((if (i % 3 == 0) NexusColors.CosmicGold else NexusColors.ElectricCyan).copy(alpha = 1f - b), 2.5f * density,
                                Offset(cx + cos(a) * r, base - b * size.height * 0.08f * (1 + i % 3) + sin(a) * r * 0.3f))
                        }
                    }
                }
            }
        }
    }
}

/** One grove tree: placed by its ground anchor on its slot, swaying gently, popping when it grows. */
@Composable
private fun GroveTreeSprite(t: GroveTree, fw: androidx.compose.ui.unit.Dp, fh: androidx.compose.ui.unit.Dp, time: State<Float>, reduce: Boolean,
                            grownAt: MutableMap<Int, TreeStage>) {
    val art = IslandLayout.trees.getOrNull(t.stage.ordinal) ?: return
    val slot = IslandLayout.treeSlots.getOrNull(t.index) ?: return
    val pop = remember { Animatable(1f) }
    LaunchedEffect(t.stage) {
        val prev = grownAt[t.index]
        grownAt[t.index] = t.stage
        if (prev != null && prev != t.stage && !reduce) { pop.snapTo(0f); pop.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    }
    val w = fw * art.w; val h = fh * art.h
    Image(painterResource(art.res), null, contentScale = ContentScale.FillBounds, modifier = Modifier
        .offset(fw * slot.x - w * art.anchorX, fh * slot.y - h * art.anchorY).size(w, h)
        .graphicsLayer {
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(art.anchorX, art.anchorY)
            val phase = t.index * 1.7f
            rotationZ = if (reduce) 0f else sin(time.value * 0.9f + phase) * (1.2f + t.stage.ordinal * 0.25f)
            val p = pop.value
            val s = if (p < 1f) 0.7f + 0.3f * p + sin(p * PI.toFloat()) * 0.12f else 1f
            scaleX = s; scaleY = s
        })
}
