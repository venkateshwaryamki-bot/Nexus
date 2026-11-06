package com.nexus.domain

import com.nexus.domain.civilization.*
import org.junit.Assert.*
import org.junit.Test

class ConstructionTest {
    @Test fun `reactor is buildable from the start`() {
        assertEquals(BuildCheck.Ok(Cost(30, 0), 1), Construction.check(BuildingType.REACTOR, 0, stage = 0, energy = 30, credits = 0))
    }
    @Test fun `locked until the civilization stage is reached`() {
        val r = Construction.check(BuildingType.DEFENSE, 0, stage = 2, energy = 9999, credits = 9999)
        assertEquals(BuildCheck.Locked(CivilizationStage.METROPOLIS), r)
    }
    @Test fun `reports exactly what is missing`() {
        val r = Construction.check(BuildingType.REACTOR, 1, stage = 0, energy = 50, credits = 2) as BuildCheck.NotEnough
        assertEquals(30, r.missingEnergy); assertEquals(3, r.missingCredits)
    }
    @Test fun `max level stops upgrades`() {
        assertEquals(BuildCheck.MaxLevel, Construction.check(BuildingType.LAB, 3, stage = 4, energy = 9999, credits = 9999))
    }
    @Test fun `every building has three increasing costs and names`() {
        BuildingType.entries.forEach { t ->
            assertTrue(t.cost(1).energy < t.cost(2).energy && t.cost(2).energy < t.cost(3).energy)
            assertEquals(3, t.levelNames.size)
        }
        assertEquals(60, Construction.xpFor(3))
    }
}
