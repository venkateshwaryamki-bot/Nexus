package com.nexus.world.scene

import kotlin.math.*

/**
 * Orbit camera for the planet view. Touch drag changes yaw/pitch (with inertia), pinch changes distance,
 * and the camera slowly drifts when idle so the world always feels alive.
 */
class OrbitCamera {
    var yaw = 0.6f
    var pitch = 0.55f
    var distance = 3.45f
    private var yawVelocity = 0f
    private var idleSeconds = 10f
    val target = Vec3(0f, 0.22f, 0f)
    var fovY = 42f

    fun eye(): Vec3 = Vec3(
        target.x + distance * cos(pitch) * sin(yaw),
        target.y + distance * sin(pitch),
        target.z + distance * cos(pitch) * cos(yaw),
    )

    fun drag(dxNorm: Float, dyNorm: Float) {
        yaw -= dxNorm * 3.2f
        pitch = (pitch + dyNorm * 2.2f).coerceIn(0.12f, 1.25f)
        yawVelocity = -dxNorm * 3.2f * 30f
        idleSeconds = 0f
    }

    fun zoom(scale: Float) { distance = (distance / scale).coerceIn(2.3f, 5.2f); idleSeconds = 0f }

    fun update(dt: Float) {
        idleSeconds += dt
        yaw += yawVelocity * dt
        yawVelocity *= exp(-dt * 3.5f)
        if (idleSeconds > 2.5f) yaw += dt * 0.06f * smoothstep(2.5f, 5f, idleSeconds)
    }

    fun view(): FloatArray = Mat4.lookAt(eye(), target, Vec3.UP)
    fun projection(aspect: Float): FloatArray {
        // Portrait phones: widen vertical FOV so the planet still fits horizontally.
        val fov = if (aspect < 1f) min(70f, fovY / aspect.pow(0.55f)) else fovY
        return Mat4.perspective(fov, aspect, 0.05f, 50f)
    }
}
