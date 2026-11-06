package com.nexus.world.scene

/**
 * The integration boundary between the Android app and whatever renders the 3D world.
 *
 * The app only ever talks to a [WorldHost]. Milestone 1 ships a native OpenGL ES 3.0 host
 * (world/gles). A future engine-backed host (e.g. Godot embedded as an Android library, see
 * TECHNICAL_DECISIONS.md) can implement the same interface without changes to app code.
 */
interface WorldHost {
    fun setWorldState(state: WorldState)
    fun setQuality(quality: GraphicsQuality)
    /** Visual feedback when energy is gained. Purely cosmetic; never affects game state. */
    fun playEnergyPulse(amount: Int)
    fun onHostResume()
    fun onHostPause()
}

/** Everything the renderer needs to know about the game. Derived from the database, never the reverse. */
data class WorldState(
    val planetId: String = "nexus_prime",
    /** 0..1 overall civilization development (continuous growth). */
    val development: Float = 0f,
    /** 0..4 CivilizationStage ordinal; drives discrete visual changes (tower tiers, ring). */
    val stage: Int = 0,
)

enum class GraphicsQuality(
    val sphereSubdivisions: Int,
    val particleCount: Int,
    /** Fraction of native resolution rendered (GPU fill-rate is the main cost on mid-range phones). */
    val renderScale: Float,
    val maxFps: Int,
    val maxStructures: Int,
) {
    LOW(4, 160, 0.6f, 30, 70),
    MEDIUM(5, 420, 0.8f, 60, 120),
    HIGH(5, 900, 1.0f, 60, 170),
}
