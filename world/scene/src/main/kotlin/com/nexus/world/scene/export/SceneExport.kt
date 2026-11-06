package com.nexus.world.scene.export

import com.nexus.world.scene.*
import java.io.File

/**
 * Developer tool (not shipped): exports scene geometry + camera matrices as JSON so the WebGL2
 * harness in tools/render-harness can render the *same* geometry with the *same* GLSL ES 3.00 shaders
 * in headless Chromium. This verifies shader compilation and visual output without a device.
 *
 * Usage: ./gradlew :world:scene:run --args="<outDir> [quality]"
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/scene-export" }).apply { mkdirs() }
    val quality = GraphicsQuality.valueOf(args.getOrElse(1) { "HIGH" })
    val cases = listOf(0f to 0, 0.06f to 1, 0.25f to 2, 0.55f to 3, 1f to 4)
    for ((dev, stage) in cases) {
        val t0 = System.nanoTime()
        val scene = PlanetScene.build(WorldState(development = dev, stage = stage), quality)
        val ms = (System.nanoTime() - t0) / 1e6
        val cam = OrbitCamera()
        val aspect = 540f / 1170f
        val view = cam.view(); val proj = cam.projection(aspect)
        val vp = Mat4.multiply(proj, view)
        val json = buildString {
            append("{")
            append("\"development\":$dev,\"stage\":$stage,")
            append("\"width\":540,\"height\":1170,")
            arr("viewProj", vp); arr("invViewProj", Mat4.invert(vp)); arr("proj", proj)
            vec("camPos", cam.eye()); vec("sunDir", RenderConstants.SunDir); vec("accent", RenderConstants.Accent)
            vec("pulseColor", RenderConstants.PulseColor); vec("nebulaA", RenderConstants.NebulaA); vec("nebulaB", RenderConstants.NebulaB)
            vec("particleA", RenderConstants.ParticleA); vec("particleB", RenderConstants.ParticleB)
            append("\"atmoStrength\":${RenderConstants.atmosphereStrength(dev)},")
            mesh("planet", scene.planet); mesh("structures", scene.structures); mesh("orbiters", scene.orbiters); mesh("atmosphere", scene.atmosphere)
            arr("lines", scene.lines); arr("orbitLines", scene.orbitLines); arr("particles", scene.particles); arr("glows", scene.glows)
            append("\"buildMs\":$ms}")
        }
        File(out, "scene_stage$stage.json").writeText(json)
        println("stage=$stage dev=$dev planetTris=${scene.planet.triangleCount} structureTris=${scene.structures.triangleCount} " +
            "orbiterTris=${scene.orbiters.triangleCount} lines=${scene.lines.size / 8} particles=${scene.particles.size / 6} " +
            "glows=${scene.glows.size / 8} build=${"%.1f".format(ms)}ms")
    }
}

private fun StringBuilder.arr(name: String, a: FloatArray) { append("\"$name\":["); a.joinTo(this, ",") { fmt(it) }; append("],") }
private fun StringBuilder.arr(name: String, a: IntArray) { append("\"$name\":["); a.joinTo(this, ","); append("],") }
private fun StringBuilder.vec(name: String, v: Vec3) = append("\"$name\":[${v.x},${v.y},${v.z}],")
private fun StringBuilder.mesh(name: String, m: Mesh) { append("\"$name\":{"); arr("v", m.vertices); setLength(length - 1); append(","); arr("i", m.indices); setLength(length - 1); append("},") }
private fun fmt(f: Float): String = if (f == f.toInt().toFloat()) f.toInt().toString() else "%.5f".format(java.util.Locale.ROOT, f)
