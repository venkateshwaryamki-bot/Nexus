package com.nexus.world.scene

import kotlin.math.*

/** CPU-side scene description, uploaded once per state change. */
class SceneData(
    val planet: Mesh,
    val structures: Mesh,
    /** Floating platforms; drawn with their own slowly rotating model matrix. */
    val orbiters: Mesh,
    val atmosphere: Mesh,
    /** Static energy-network line segments: x,y,z,intensity per vertex, two vertices per segment. */
    val lines: FloatArray,
    /** Orbital ring line segments, rotating with [orbiters]. Same layout as [lines]. */
    val orbitLines: FloatArray,
    /** Particles: radius, phase, y, speed, size, colorMix (6 floats each). Animated in the vertex shader. */
    val particles: FloatArray,
    /** Glow sprites (fake bloom): x,y,z,size, r,g,b, phase (8 floats each). */
    val glows: FloatArray,
    val towerTop: Vec3,
    val development: Float,
    val stage: Int,
) {
    companion object {
        const val LINE_STRIDE = 4
        const val PARTICLE_STRIDE = 6
        const val GLOW_STRIDE = 8
    }
}

/**
 * Procedurally builds Nexus Prime for a given development level. Deterministic: the same inputs
 * always produce the same planet, so growth is stable — a building that exists at 30% development
 * still stands, in the same place, at 31%.
 *
 * NOTE: this is polished *procedural placeholder art* for Milestone 1 (see ASSET_PIPELINE.md).
 * Production meshes authored in Blender will replace the building generators, while placement,
 * growth logic and the WorldHost boundary stay.
 */
object PlanetScene {
    const val SEED = 0x4E58 // "NX"
    const val SEA_LEVEL = 0.47f
    private val noise = Noise3(SEED)
    private val cityNoise = Noise3(SEED + 7)

    fun terrainHeight(p: Vec3): Float = noise.fbm(p * 1.7f, 5)

    fun build(state: WorldState, quality: GraphicsQuality): SceneData {
        val dev = state.development.coerceIn(0f, 1f)
        val stage = state.stage.coerceIn(0, 4)
        val planet = buildPlanet(quality.sphereSubdivisions)
        val structures = MeshBuilder()
        val lines = FloatList(); val orbitLines = FloatList(); val glows = FloatList()

        // --- Nexus Tower (the headquarters), standing on the north pole -------------------------
        val towerTop = buildTower(structures, glows, dev, stage)

        // --- City: grows outward from the tower ---------------------------------------------------
        val sites = citySites()
        val count = (mix(8f, quality.maxStructures.toFloat(), dev.pow(0.8f))).roundToInt().coerceAtMost(sites.size)
        val alive = smoothstep(0f, 0.04f, dev) // 0 = abandoned outpost
        val placed = ArrayList<Vec3>()
        for (idx in 0 until count) {
            val site = sites[idx]
            val surface = site * surfaceRadius(site)
            val angle = acos(site.y.coerceIn(-1f, 1f))
            val h = hash(idx)
            val heightScale = (0.5f + 0.7f * dev) * (1.25f - angle * 0.5f) * (0.75f + 0.5f * hash(idx + 991))
            buildStructure(structures, glows, surface, site, idx, h, heightScale, dev, alive, stage)
            // Energy line to the nearest already-placed structure closer to the tower (or the tower itself).
            val target = placed.minByOrNull { (it - surface).length() }?.takeIf { (it - surface).length() < 0.32f }
                ?: Vec3(0f, surfaceRadius(Vec3.UP), 0f)
            arc(lines, surface, target, intensity = mix(0.06f, 1f, alive) * (0.35f + 0.65f * dev))
            placed.add(surface)
        }

        // --- Orbit: floating stations and rings appear at higher stages ----------------------------
        val orbiters = MeshBuilder()
        val platformCount = when (stage) { 0, 1 -> 0; 2 -> 3; 3 -> 6; else -> 10 }
        for (k in 0 until platformCount) {
            val a = k * (2 * PI / platformCount).toFloat() + 0.4f
            val r = 1.38f + 0.08f * hash(k + 50)
            val y = 0.15f + 0.35f * hash(k + 70)
            val c = Vec3(cos(a) * r, y, sin(a) * r)
            val outward = Vec3(cos(a), 0f, sin(a))
            orbiters.prism(c, Vec3.UP, outward, 6, 0.05f, 0.035f, 0.018f, Vec3(0.55f, 0.62f, 0.72f), 0.25f, 0.9f)
            orbiters.prism(c - Vec3.UP * 0.03f, Vec3.UP, outward, 6, 0.012f, 0.035f, 0.03f, Vec3(0.3f, 0.34f, 0.42f), 0.6f, 0f)
            glows.add(c.x, c.y + 0.03f, c.z, 0.05f); glows.addColor(Palette.ElectricCyan); glows.add(hash(k) * 6f)
        }
        if (stage >= 3) ring(orbitLines, 1.62f, 0.12f, if (stage == 4) 1f else 0.6f)
        if (stage >= 4) ring(orbitLines, 1.78f, -0.2f, 0.45f)

        return SceneData(
            planet = planet, structures = structures.build(), orbiters = orbiters.build(),
            atmosphere = buildAtmosphere(),
            lines = lines.toArray(), orbitLines = orbitLines.toArray(),
            particles = buildParticles(quality.particleCount, dev), glows = glows.toArray(),
            towerTop = towerTop, development = dev, stage = stage,
        )
    }

    fun surfaceRadius(dir: Vec3): Float {
        val h = terrainHeight(dir)
        return 1f + if (h > SEA_LEVEL) (h - SEA_LEVEL) * 0.22f else 0f
    }

    private fun buildPlanet(subdiv: Int): Mesh {
        val (dirs, faces) = Icosphere.generate(subdiv)
        val pos = dirs.map { it * surfaceRadius(it) }
        // Smooth normals from displaced geometry.
        val nrm = Array(pos.size) { Vec3.ZERO }
        for (f in faces.indices step 3) {
            val a = faces[f]; val b = faces[f + 1]; val c = faces[f + 2]
            val n = (pos[b] - pos[a]) cross (pos[c] - pos[a])
            nrm[a] = nrm[a] + n; nrm[b] = nrm[b] + n; nrm[c] = nrm[c] + n
        }
        val deep = Vec3(0.012f, 0.045f, 0.11f); val shallow = Vec3(0.03f, 0.17f, 0.33f)
        val lowland = Vec3(0.13f, 0.17f, 0.25f); val highland = Vec3(0.34f, 0.40f, 0.52f)
        val mb = MeshBuilder()
        for (k in dirs.indices) {
            val d = dirs[k]; val h = terrainHeight(d)
            val land = h > SEA_LEVEL
            val color = if (land) mix(lowland, highland, smoothstep(SEA_LEVEL, 0.68f, h))
            else mix(deep, shallow, smoothstep(0.30f, SEA_LEVEL, h))
            // Planet emissive channel = city-light mask; the shader reveals it as development grows,
            // spreading from the pole (weighted by latitude).
            val city = if (land) (cityNoise.fbm(d * 9f, 3) * 0.65f + (d.y * 0.5f + 0.5f) * 0.45f).coerceIn(0f, 1f) else 0f
            mb.vertex(pos[k], nrm[k].normalized(), color, city)
        }
        for (f in faces.indices step 3) mb.tri(faces[f], faces[f + 1], faces[f + 2])
        return mb.build()
    }

    private fun buildAtmosphere(): Mesh {
        val (dirs, faces) = Icosphere.generate(3)
        val mb = MeshBuilder()
        dirs.forEach { mb.vertex(it * 1.09f, it, Palette.ElectricCyan, 1f) }
        for (f in faces.indices step 3) mb.tri(faces[f], faces[f + 1], faces[f + 2])
        return mb.build()
    }

    private fun buildTower(mb: MeshBuilder, glows: FloatList, dev: Float, stage: Int): Vec3 {
        val base = Vec3(0f, surfaceRadius(Vec3.UP) - 0.01f, 0f)
        val t = Vec3(1f, 0f, 0f)
        val metal = Vec3(0.42f, 0.48f, 0.58f); val dark = Vec3(0.16f, 0.19f, 0.25f)
        val lit = smoothstep(0f, 0.04f, dev)
        val height = 0.16f + 0.07f * stage + 0.05f * dev
        // Plinth, core shaft, tapered spire, and energy rings that multiply with each stage.
        mb.prism(base, Vec3.UP, t, 8, 0.12f, 0.1f, 0.03f, dark, 0.05f + 0.3f * lit, 0.1f)
        mb.prism(base + Vec3.UP * 0.03f, Vec3.UP, t, 8, 0.05f, 0.035f, height * 0.7f, metal, 0.12f + 0.5f * lit, 0f, 0.39f)
        mb.prism(base + Vec3.UP * (0.03f + height * 0.7f), Vec3.UP, t, 8, 0.035f, 0.004f, height * 0.45f, metal, 0.2f + 0.7f * lit, 0f, 0.39f)
        for (r in 0 until stage + 1) {
            val y = 0.05f + r * (height * 0.65f / (stage + 1))
            mb.prism(base + Vec3.UP * y, Vec3.UP, t, 16, 0.075f - r * 0.006f, 0.075f - r * 0.006f, 0.006f,
                Palette.ElectricCyan, 0.15f + 0.85f * lit, 0.15f + 0.85f * lit)
        }
        val top = base + Vec3.UP * (0.03f + height * 1.15f)
        glows.add(top.x, top.y, top.z, 0.10f + 0.22f * lit + 0.06f * stage)
        glows.addColor(if (lit > 0.5f) Palette.ElectricCyan else Vec3(0.2f, 0.35f, 0.45f)); glows.add(0f)
        return top
    }

    private fun buildStructure(
        mb: MeshBuilder, glows: FloatList, surface: Vec3, up: Vec3, idx: Int, h: Float, s: Float,
        dev: Float, alive: Float, stage: Int,
    ) {
        val tangent = (Vec3.UP cross up).let { if (it.length() < 1e-3f) Vec3(1f, 0f, 0f) else it.normalized() }
        val base = surface - up * 0.008f
        val ruin = Vec3(0.17f, 0.19f, 0.23f)
        val hull = mix(ruin, Vec3(0.46f, 0.52f, 0.62f), alive)
        val windows = mix(0.04f, 0.55f + 0.4f * h, alive)
        val rot = h * 6.28f
        // Type distribution shifts toward taller, more advanced architecture as stages progress.
        val roll = (h + stage * 0.12f) % 1f
        when {
            roll < 0.34f -> { // Habitat: hexagonal dome cluster
                mb.prism(base, up, tangent, 6, 0.045f, 0.03f, 0.03f * s, hull, windows * 0.6f, 0.05f, rot)
                mb.prism(base + up * (0.03f * s), up, tangent, 6, 0.03f, 0.008f, 0.018f, hull, 0f, windows, rot)
            }
            roll < 0.6f -> { // Research lab: block + antenna
                mb.prism(base, up, tangent, 4, 0.045f, 0.04f, 0.05f * s, hull, windows, 0.1f, rot)
                mb.prism(base + up * (0.05f * s), up, tangent, 4, 0.004f, 0.002f, 0.05f * s, hull, alive, 0f, rot)
                if (alive > 0.5f && idx % 3 == 0) {
                    val tip = base + up * (0.1f * s)
                    glows.add(tip.x, tip.y, tip.z, 0.035f); glows.addColor(Palette.NeonGreen); glows.add(h * 9f)
                }
            }
            roll < 0.85f -> { // Spire
                mb.prism(base, up, tangent, 4, 0.03f, 0.009f, 0.085f * s, hull, windows, 0f, rot)
            }
            else -> { // Energy reactor with glowing core
                mb.prism(base, up, tangent, 12, 0.05f, 0.045f, 0.018f * s, hull, 0.1f, 0.05f, rot)
                mb.prism(base + up * (0.018f * s), up, tangent, 12, 0.022f, 0.018f, 0.025f, Palette.ElectricCyan, alive, alive, rot)
                if (alive > 0.5f) {
                    val c = base + up * (0.04f * s)
                    glows.add(c.x, c.y, c.z, 0.07f + 0.04f * dev); glows.addColor(Palette.ElectricCyan); glows.add(h * 7f)
                }
            }
        }
    }

    /** Land sites on a Fibonacci lattice, sorted by distance from the tower (north pole). */
    private val siteCache by lazy {
        val n = 1400
        val golden = PI * (3 - sqrt(5.0))
        (0 until n).map { i ->
            val y = 1 - (i + 0.5f) / n * 2
            val r = sqrt(1 - y * y)
            val th = (golden * i).toFloat()
            Vec3(cos(th) * r, y, sin(th) * r)
        }.filter { acos(it.y) > 0.17f && acos(it.y) < 1.45f && terrainHeight(it) > SEA_LEVEL + 0.01f }
            .sortedBy { acos(it.y) + 0.12f * hash((it.x * 1000).toInt()) }
    }
    fun citySites(): List<Vec3> = siteCache

    private fun arc(out: FloatList, a: Vec3, b: Vec3, intensity: Float) {
        val steps = 6
        var prev = a.normalized() * (a.length() + 0.004f)
        for (k in 1..steps) {
            val t = k / steps.toFloat()
            val d = mix(a, b, t).normalized()
            val p = d * (mix(a.length(), b.length(), t) + 0.004f + sin(t * PI.toFloat()) * 0.01f)
            out.add(prev.x, prev.y, prev.z, intensity); out.add(p.x, p.y, p.z, intensity)
            prev = p
        }
    }

    private fun ring(out: FloatList, radius: Float, tilt: Float, intensity: Float) {
        val n = 96
        fun p(k: Int): Vec3 {
            val a = k * 2 * PI.toFloat() / n
            val x = cos(a) * radius; val z = sin(a) * radius
            return Vec3(x, z * sin(tilt), z * cos(tilt))
        }
        for (k in 0 until n) { val a = p(k); val b = p(k + 1)
            out.add(a.x, a.y, a.z, intensity); out.add(b.x, b.y, b.z, intensity) }
    }

    private fun buildParticles(count: Int, dev: Float): FloatArray {
        val rnd = java.util.Random(SEED.toLong())
        val active = (count * (0.25f + 0.75f * dev)).toInt().coerceAtLeast(24)
        val out = FloatArray(active * SceneData.PARTICLE_STRIDE)
        for (i in 0 until active) {
            val r = 1.1f + rnd.nextFloat().pow(1.6f) * 0.9f
            out[i * 6] = r
            out[i * 6 + 1] = rnd.nextFloat() * 6.2832f
            out[i * 6 + 2] = (rnd.nextFloat() * 1.5f - 0.6f) * r
            out[i * 6 + 3] = (0.02f + rnd.nextFloat() * 0.08f) * (if (rnd.nextBoolean()) 1 else -1)
            out[i * 6 + 4] = 1.5f + rnd.nextFloat() * 3.5f
            out[i * 6 + 5] = rnd.nextFloat()
        }
        return out
    }

    private fun hash(i: Int): Float {
        var x = i * 374761393 + 668265263
        x = (x xor (x ushr 13)) * 1274126177
        return ((x xor (x ushr 16)) and 0xFFFFFF) / 16777215f
    }
}

/** Minimal growable float list (avoids boxing). */
class FloatList {
    private var a = FloatArray(1024); var size = 0; private set
    fun add(vararg v: Float) { if (size + v.size > a.size) a = a.copyOf(max(a.size * 2, size + v.size)); v.copyInto(a, size); size += v.size }
    fun addColor(c: Vec3) = add(c.x, c.y, c.z)
    fun toArray() = a.copyOf(size)
}
