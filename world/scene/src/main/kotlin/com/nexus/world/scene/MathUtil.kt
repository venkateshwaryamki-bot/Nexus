package com.nexus.world.scene

import kotlin.math.*

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(this dot this)
    fun normalized(): Vec3 { val l = length(); return if (l < 1e-8f) this else this * (1f / l) }
    companion object { val UP = Vec3(0f, 1f, 0f); val ZERO = Vec3(0f, 0f, 0f) }
}

/** Column-major 4x4 matrices (OpenGL convention), stored in FloatArray(16). */
object Mat4 {
    fun identity() = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }

    fun perspective(fovYDeg: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = 1f / tan(Math.toRadians(fovYDeg / 2.0)).toFloat()
        return FloatArray(16).also {
            it[0] = f / aspect; it[5] = f
            it[10] = (far + near) / (near - far); it[11] = -1f
            it[14] = 2f * far * near / (near - far)
        }
    }

    fun lookAt(eye: Vec3, target: Vec3, up: Vec3): FloatArray {
        val f = (target - eye).normalized()
        val s = (f cross up).normalized()
        val u = s cross f
        return floatArrayOf(
            s.x, u.x, -f.x, 0f,
            s.y, u.y, -f.y, 0f,
            s.z, u.z, -f.z, 0f,
            -(s dot eye), -(u dot eye), f dot eye, 1f,
        )
    }

    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(16)
        for (c in 0 until 4) for (row in 0 until 4) {
            var v = 0f
            for (k in 0 until 4) v += a[k * 4 + row] * b[c * 4 + k]
            r[c * 4 + row] = v
        }
        return r
    }

    fun rotationY(rad: Float): FloatArray = identity().also {
        val c = cos(rad); val s = sin(rad)
        it[0] = c; it[2] = -s; it[8] = s; it[10] = c
    }

    /** General inverse (used once per frame for the sky ray reconstruction). */
    fun invert(m: FloatArray): FloatArray {
        val inv = FloatArray(16)
        inv[0] = m[5]*m[10]*m[15]-m[5]*m[11]*m[14]-m[9]*m[6]*m[15]+m[9]*m[7]*m[14]+m[13]*m[6]*m[11]-m[13]*m[7]*m[10]
        inv[4] = -m[4]*m[10]*m[15]+m[4]*m[11]*m[14]+m[8]*m[6]*m[15]-m[8]*m[7]*m[14]-m[12]*m[6]*m[11]+m[12]*m[7]*m[10]
        inv[8] = m[4]*m[9]*m[15]-m[4]*m[11]*m[13]-m[8]*m[5]*m[15]+m[8]*m[7]*m[13]+m[12]*m[5]*m[11]-m[12]*m[7]*m[9]
        inv[12] = -m[4]*m[9]*m[14]+m[4]*m[10]*m[13]+m[8]*m[5]*m[14]-m[8]*m[6]*m[13]-m[12]*m[5]*m[10]+m[12]*m[6]*m[9]
        inv[1] = -m[1]*m[10]*m[15]+m[1]*m[11]*m[14]+m[9]*m[2]*m[15]-m[9]*m[3]*m[14]-m[13]*m[2]*m[11]+m[13]*m[3]*m[10]
        inv[5] = m[0]*m[10]*m[15]-m[0]*m[11]*m[14]-m[8]*m[2]*m[15]+m[8]*m[3]*m[14]+m[12]*m[2]*m[11]-m[12]*m[3]*m[10]
        inv[9] = -m[0]*m[9]*m[15]+m[0]*m[11]*m[13]+m[8]*m[1]*m[15]-m[8]*m[3]*m[13]-m[12]*m[1]*m[11]+m[12]*m[3]*m[9]
        inv[13] = m[0]*m[9]*m[14]-m[0]*m[10]*m[13]-m[8]*m[1]*m[14]+m[8]*m[2]*m[13]+m[12]*m[1]*m[10]-m[12]*m[2]*m[9]
        inv[2] = m[1]*m[6]*m[15]-m[1]*m[7]*m[14]-m[5]*m[2]*m[15]+m[5]*m[3]*m[14]+m[13]*m[2]*m[7]-m[13]*m[3]*m[6]
        inv[6] = -m[0]*m[6]*m[15]+m[0]*m[7]*m[14]+m[4]*m[2]*m[15]-m[4]*m[3]*m[14]-m[12]*m[2]*m[7]+m[12]*m[3]*m[6]
        inv[10] = m[0]*m[5]*m[15]-m[0]*m[7]*m[13]-m[4]*m[1]*m[15]+m[4]*m[3]*m[13]+m[12]*m[1]*m[7]-m[12]*m[3]*m[5]
        inv[14] = -m[0]*m[5]*m[14]+m[0]*m[6]*m[13]+m[4]*m[1]*m[14]-m[4]*m[2]*m[13]-m[12]*m[1]*m[6]+m[12]*m[2]*m[5]
        inv[3] = -m[1]*m[6]*m[11]+m[1]*m[7]*m[10]+m[5]*m[2]*m[11]-m[5]*m[3]*m[10]-m[9]*m[2]*m[7]+m[9]*m[3]*m[6]
        inv[7] = m[0]*m[6]*m[11]-m[0]*m[7]*m[10]-m[4]*m[2]*m[11]+m[4]*m[3]*m[10]+m[8]*m[2]*m[7]-m[8]*m[3]*m[6]
        inv[11] = -m[0]*m[5]*m[11]+m[0]*m[7]*m[9]+m[4]*m[1]*m[11]-m[4]*m[3]*m[9]-m[8]*m[1]*m[7]+m[8]*m[3]*m[5]
        inv[15] = m[0]*m[5]*m[10]-m[0]*m[6]*m[9]-m[4]*m[1]*m[10]+m[4]*m[2]*m[9]+m[8]*m[1]*m[6]-m[8]*m[2]*m[5]
        var det = m[0]*inv[0]+m[1]*inv[4]+m[2]*inv[8]+m[3]*inv[12]
        if (abs(det) < 1e-12f) return identity()
        det = 1f / det
        for (i in 0 until 16) inv[i] *= det
        return inv
    }
}

/** Deterministic 3D value noise + fBm. Seeded so every device builds the identical planet. */
class Noise3(seed: Int) {
    private val perm = IntArray(512)
    init {
        val p = IntArray(256) { it }
        val rnd = java.util.Random(seed.toLong())
        for (i in 255 downTo 1) { val j = rnd.nextInt(i + 1); val t = p[i]; p[i] = p[j]; p[j] = t }
        for (i in 0 until 512) perm[i] = p[i and 255]
    }
    private fun hash(x: Int, y: Int, z: Int) = perm[perm[perm[x and 255] + (y and 255)] + (z and 255)] / 255f
    private fun fade(t: Float) = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun value(x: Float, y: Float, z: Float): Float {
        val xi = floor(x).toInt(); val yi = floor(y).toInt(); val zi = floor(z).toInt()
        val xf = fade(x - xi); val yf = fade(y - yi); val zf = fade(z - zi)
        val c000 = hash(xi, yi, zi); val c100 = hash(xi + 1, yi, zi)
        val c010 = hash(xi, yi + 1, zi); val c110 = hash(xi + 1, yi + 1, zi)
        val c001 = hash(xi, yi, zi + 1); val c101 = hash(xi + 1, yi, zi + 1)
        val c011 = hash(xi, yi + 1, zi + 1); val c111 = hash(xi + 1, yi + 1, zi + 1)
        return lerp(lerp(lerp(c000, c100, xf), lerp(c010, c110, xf), yf),
            lerp(lerp(c001, c101, xf), lerp(c011, c111, xf), yf), zf)
    }

    fun fbm(p: Vec3, octaves: Int = 5): Float {
        var amp = 0.5f; var freq = 1f; var sum = 0f; var norm = 0f
        repeat(octaves) {
            sum += amp * value(p.x * freq, p.y * freq, p.z * freq); norm += amp
            amp *= 0.5f; freq *= 2.03f
        }
        return sum / norm
    }
}

fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f); return t * t * (3 - 2 * t)
}
fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
fun mix(a: Vec3, b: Vec3, t: Float) = Vec3(mix(a.x, b.x, t), mix(a.y, b.y, t), mix(a.z, b.z, t))
