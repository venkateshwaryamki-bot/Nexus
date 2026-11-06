package com.nexus.domain.civilization

enum class CivilizationStage(val title: String, val requiredEnergy: Int, val description: String) {
    ABANDONED_OUTPOST("Abandoned Outpost", 0, "Dark towers and a dormant Nexus Core. Power is gone."),
    SMALL_SETTLEMENT("Small Settlement", 60, "The first habitats flicker back to life."),
    RESEARCH_COLONY("Research Colony", 300, "Laboratories and energy lines spread across the surface."),
    METROPOLIS("Futuristic Metropolis", 1200, "Towers rise and the night side glows with cities."),
    PLANETARY_CIVILIZATION("Planetary Civilization", 3000, "Nexus Prime is fully restored and radiant."),
}

data class CivilizationStatus(
    val stage: CivilizationStage,
    val nextStage: CivilizationStage?,
    val developmentEnergy: Int,
    /** 0..1 progress toward [nextStage]; 1 when fully developed. */
    val progressToNext: Float,
    /** 0..1 overall development, used by the renderer for continuous growth between stages. */
    val overallDevelopment: Float,
)

/**
 * Milestone 1: Nexus Prime develops from the total energy channelled into it (lifetime energy).
 * Milestone 2 adds buildings purchased with the energy balance on top of this baseline.
 */
object Civilization {
    fun statusFor(energy: Int): CivilizationStatus {
        val developmentEnergy = energy.coerceAtLeast(0)
        val stages = CivilizationStage.entries
        val stage = stages.last { developmentEnergy >= it.requiredEnergy }
        val next = stages.getOrNull(stage.ordinal + 1)
        val progress = if (next == null) 1f else
            (developmentEnergy - stage.requiredEnergy).toFloat() / (next.requiredEnergy - stage.requiredEnergy)
        val max = stages.last().requiredEnergy.toFloat()
        return CivilizationStatus(
            stage, next, developmentEnergy, progress.coerceIn(0f, 1f),
            (developmentEnergy / max).coerceIn(0f, 1f),
        )
    }
}
