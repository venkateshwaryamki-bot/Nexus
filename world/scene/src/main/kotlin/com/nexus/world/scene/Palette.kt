package com.nexus.world.scene

/** NEXUS colour tokens in linear-ish RGB for the renderer (see ART_DIRECTION.md). */
object Palette {
    val DeepSpaceNavy = hex(0x061321)
    val MidnightBlue = hex(0x0B2035)
    val ElectricCyan = hex(0x27DFFF)
    val NeonGreen = hex(0x43F58A)
    val CosmicGold = hex(0xF5C85B)
    val Purple = hex(0x9275FF)

    fun hex(rgb: Int) = Vec3(((rgb shr 16) and 255) / 255f, ((rgb shr 8) and 255) / 255f, (rgb and 255) / 255f)
}
