package com.nexus.domain.civilization

data class Cost(val energy: Int, val credits: Int)

/**
 * Player-built structures on Nexus Prime (GAME_DESIGN.md §Civilization buildings).
 * Three levels each; every level changes the island art. Energy is *spent* from the balance —
 * planet development (lifetime energy) is never reduced by building.
 */
enum class BuildingType(val title: String, val description: String, val unlockStage: Int, private val costs: List<Cost>, val levelNames: List<String>) {
    REACTOR("Energy Reactor", "Stabilises the planet's power grid. The first structure to bring back.", 0,
        listOf(Cost(30, 0), Cost(80, 5), Cost(180, 15)), listOf("Core", "Cooled Core", "Fusion Dome")),
    COLONY("Residential Colony", "Homes for returning settlers under glass habitat domes.", 1,
        listOf(Cost(50, 0), Cost(120, 8), Cost(250, 20)), listOf("Habitat", "Twin Habitats", "Colony Quarter")),
    LAB("Research Laboratory", "Studies the Nexus Core. Grows from a lab into a full Tech Hub.", 1,
        listOf(Cost(60, 0), Cost(150, 10), Cost(300, 25)), listOf("Outpost Lab", "Research Lab", "Tech Hub")),
    COMMS("Communication Centre", "Reconnects Nexus Prime to the rest of the galaxy.", 2,
        listOf(Cost(90, 5), Cost(200, 15), Cost(400, 30)), listOf("Relay Mast", "Dish Array", "Signal Spire")),
    SPACEPORT("Spaceport", "Landing pads for explorers. Opens the way to other worlds.", 2,
        listOf(Cost(120, 10), Cost(260, 20), Cost(500, 40)), listOf("Landing Pad", "Control Tower", "Starship Dock")),
    DEFENSE("Defense Tower", "Shields the city from cosmic storms.", 3,
        listOf(Cost(150, 10), Cost(320, 25), Cost(600, 45)), listOf("Sentry", "Pylon Grid", "Shield Dome"));

    /** Cost to go from [toLevel]-1 to [toLevel] (1..MAX_LEVEL). */
    fun cost(toLevel: Int): Cost = costs[toLevel - 1]

    companion object { const val MAX_LEVEL = 3 }
}

sealed interface BuildCheck {
    data class Ok(val cost: Cost, val toLevel: Int) : BuildCheck
    data class Locked(val requiredStage: CivilizationStage) : BuildCheck
    data object MaxLevel : BuildCheck
    data class NotEnough(val cost: Cost, val missingEnergy: Int, val missingCredits: Int) : BuildCheck
}

object Construction {
    /** XP for completing a construction level (rewards building as well as focusing). */
    fun xpFor(level: Int) = 20 * level

    fun check(type: BuildingType, currentLevel: Int, stage: Int, energy: Long, credits: Long): BuildCheck {
        if (currentLevel >= BuildingType.MAX_LEVEL) return BuildCheck.MaxLevel
        if (stage < type.unlockStage) return BuildCheck.Locked(CivilizationStage.entries[type.unlockStage])
        val c = type.cost(currentLevel + 1)
        val me = (c.energy - energy).coerceAtLeast(0).toInt(); val mc = (c.credits - credits).coerceAtLeast(0).toInt()
        return if (me > 0 || mc > 0) BuildCheck.NotEnough(c, me, mc) else BuildCheck.Ok(c, currentLevel + 1)
    }
}
