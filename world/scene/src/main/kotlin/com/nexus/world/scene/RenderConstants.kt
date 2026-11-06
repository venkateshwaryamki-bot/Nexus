package com.nexus.world.scene

/** Lighting/colour constants shared by the Android renderer and the WebGL verification harness. */
object RenderConstants {
    val SunDir = Vec3(-0.75f, 0.55f, 0.45f).normalized()
    val Accent = Palette.ElectricCyan
    val PulseColor = Palette.NeonGreen
    val NebulaA = Vec3(0.05f, 0.30f, 0.55f)
    val NebulaB = Palette.Purple * 0.6f
    val ParticleA = Palette.ElectricCyan
    val ParticleB = Palette.NeonGreen
    const val PULSE_SECONDS = 2.8f
    const val ORBIT_SPEED = 0.05f // rad/s for floating stations
    fun atmosphereStrength(development: Float) = 0.55f + 0.6f * development
}
