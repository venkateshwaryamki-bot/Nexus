package com.nexus.world.scene

import org.junit.Assert.*
import org.junit.Test

class PlanetSceneTest {
    @Test fun `scene is deterministic`() {
        val a = PlanetScene.build(WorldState(development = 0.3f, stage = 2), GraphicsQuality.MEDIUM)
        val b = PlanetScene.build(WorldState(development = 0.3f, stage = 2), GraphicsQuality.MEDIUM)
        assertArrayEquals(a.structures.vertices, b.structures.vertices, 0f)
        assertArrayEquals(a.planet.indices, b.planet.indices)
    }

    @Test fun `civilization visibly grows with development`() {
        val q = GraphicsQuality.HIGH
        val counts = listOf(0f to 0, 0.06f to 1, 0.25f to 2, 0.55f to 3, 1f to 4).map { (d, s) ->
            PlanetScene.build(WorldState(development = d, stage = s), q).structures.triangleCount
        }
        for (k in 1 until counts.size) assertTrue("structures must grow: $counts", counts[k] > counts[k - 1])
    }

    @Test fun `orbital stations unlock at research colony`() {
        assertEquals(0, PlanetScene.build(WorldState(development = 0.06f, stage = 1), GraphicsQuality.LOW).orbiters.triangleCount)
        assertTrue(PlanetScene.build(WorldState(development = 0.3f, stage = 2), GraphicsQuality.LOW).orbiters.triangleCount > 0)
    }

    @Test fun `quality tiers bound geometry budgets`() {
        val low = PlanetScene.build(WorldState(development = 1f, stage = 4), GraphicsQuality.LOW)
        val high = PlanetScene.build(WorldState(development = 1f, stage = 4), GraphicsQuality.HIGH)
        assertTrue(low.planet.triangleCount < high.planet.triangleCount)
        assertTrue(low.particles.size < high.particles.size)
        // Mid-range budget: keep the full scene well under 100k triangles even on HIGH.
        val total = high.planet.triangleCount + high.structures.triangleCount + high.orbiters.triangleCount
        assertTrue("triangles=$total", total < 100_000)
    }

    @Test fun `matrix inverse round trips`() {
        val cam = OrbitCamera()
        val vp = Mat4.multiply(cam.projection(0.5f), cam.view())
        val id = Mat4.multiply(vp, Mat4.invert(vp))
        for (k in 0 until 16) assertEquals(if (k % 5 == 0) 1f else 0f, id[k], 1e-3f)
    }

    @Test fun `vertex data contains no NaN`() {
        val s = PlanetScene.build(WorldState(development = 1f, stage = 4), GraphicsQuality.HIGH)
        listOf(s.planet.vertices, s.structures.vertices, s.lines, s.glows, s.particles).forEach { a ->
            assertTrue(a.none { it.isNaN() || it.isInfinite() })
        }
    }
}
