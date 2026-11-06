package com.nexus.domain

import com.nexus.domain.civilization.Grove
import com.nexus.domain.civilization.TreeGrowth
import com.nexus.domain.civilization.TreeStage
import org.junit.Assert.*
import org.junit.Test

class GroveTest {
    @Test fun `day one has a single seed`() {
        val g = Grove.compute(0)
        assertEquals(1, g.size); assertEquals(TreeStage.SEED, g[0].stage); assertEquals(25, g[0].minutesToNext)
    }
    @Test fun `stages follow completed minutes`() {
        assertEquals(TreeStage.SPROUT, Grove.compute(25)[0].stage)
        assertEquals(TreeStage.YOUNG, Grove.compute(80)[0].stage)
        assertEquals(TreeStage.MATURE, Grove.compute(150)[0].stage)
        assertEquals(TreeStage.ANCIENT, Grove.compute(300)[0].stage)
        assertEquals(1f, Grove.compute(999)[0].progressToNext)
    }
    @Test fun `a new seed every two and a half hours, capped`() {
        val g = Grove.compute(310)
        assertEquals(3, g.size)
        assertEquals(listOf(TreeStage.ANCIENT, TreeStage.MATURE, TreeStage.SEED), g.map { it.stage }) // 310, 160, 10 min
        assertEquals(Grove.MAX_TREES, Grove.compute(100_000).size)
    }
    @Test fun `growth reports stage changes and new seeds`() {
        assertEquals(listOf(TreeGrowth(0, TreeStage.SEED, TreeStage.SPROUT)), Grove.growth(0, 25))
        assertEquals(emptyList<TreeGrowth>(), Grove.growth(30, 40))
        val g = Grove.growth(140, 160)
        assertTrue(TreeGrowth(0, TreeStage.YOUNG, TreeStage.MATURE) in g)
        assertTrue(TreeGrowth(1, null, TreeStage.SEED) in g)
    }
    @Test fun `negative input is safe`() { assertEquals(1, Grove.compute(-5).size) }
}
