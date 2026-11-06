package com.nexus.world.scene

/**
 * Interleaved triangle mesh. Vertex layout (10 floats, see [STRIDE]):
 *   position.xyz, normal.xyz, albedo.rgb, emissive (0..1, meaning depends on material)
 */
class Mesh(val vertices: FloatArray, val indices: IntArray) {
    val vertexCount get() = vertices.size / STRIDE
    val triangleCount get() = indices.size / 3
    companion object { const val STRIDE = 10 }
}

class MeshBuilder {
    private var v = FloatArray(4096); private var vn = 0
    private var i = IntArray(4096); private var inn = 0
    val vertexCount get() = vn / Mesh.STRIDE

    fun vertex(p: Vec3, n: Vec3, c: Vec3, emissive: Float): Int {
        if (vn + Mesh.STRIDE > v.size) v = v.copyOf(v.size * 2)
        v[vn++] = p.x; v[vn++] = p.y; v[vn++] = p.z
        v[vn++] = n.x; v[vn++] = n.y; v[vn++] = n.z
        v[vn++] = c.x; v[vn++] = c.y; v[vn++] = c.z
        v[vn++] = emissive
        return vertexCount - 1
    }

    fun tri(a: Int, b: Int, c: Int) {
        if (inn + 3 > i.size) i = i.copyOf(i.size * 2)
        i[inn++] = a; i[inn++] = b; i[inn++] = c
    }

    /** Flat-shaded quad (a,b,c,d counter-clockwise when viewed from outside). */
    fun quad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, color: Vec3, em: Float) {
        val n = ((b - a) cross (c - a)).normalized()
        val ia = vertex(a, n, color, em); val ib = vertex(b, n, color, em)
        val ic = vertex(c, n, color, em); val id = vertex(d, n, color, em)
        tri(ia, ib, ic); tri(ia, ic, id)
    }

    /**
     * Tapered n-sided prism standing on [base] along [up]. Used for every architectural element:
     * n=4 gives towers/blocks, n=6 habitats, n=12+ approximates cylinders and domes.
     * [sideEmissive] lights the walls (windows/energy bands), [capEmissive] the roof.
     */
    fun prism(
        base: Vec3, up: Vec3, tangent: Vec3, sides: Int, radiusBottom: Float, radiusTop: Float,
        height: Float, color: Vec3, sideEmissive: Float, capEmissive: Float, rotation: Float = 0f,
    ) {
        val u = up.normalized(); val t = (tangent - u * (tangent dot u)).normalized(); val b = u cross t
        fun ring(r: Float, h: Float) = List(sides) { k ->
            val a = rotation + k * (2 * Math.PI / sides).toFloat()
            base + u * h + t * (r * kotlin.math.cos(a)) + b * (r * kotlin.math.sin(a))
        }
        val bottom = ring(radiusBottom, 0f); val top = ring(radiusTop, height)
        for (k in 0 until sides) {
            val k2 = (k + 1) % sides
            quad(bottom[k], bottom[k2], top[k2], top[k], color, sideEmissive)
        }
        if (radiusTop > 1e-4f) {
            val center = base + u * height
            val ic = vertex(center, u, color, capEmissive)
            val ring = top.map { vertex(it, u, color, capEmissive) }
            for (k in 0 until sides) tri(ic, ring[k], ring[(k + 1) % sides])
        }
    }

    fun build() = Mesh(v.copyOf(vn), i.copyOf(inn))
}

/** Subdivided icosahedron; returns unit-sphere vertices and triangle indices. */
object Icosphere {
    fun generate(subdivisions: Int): Pair<List<Vec3>, IntArray> {
        val t = ((1.0 + kotlin.math.sqrt(5.0)) / 2.0).toFloat()
        val verts = mutableListOf(
            Vec3(-1f, t, 0f), Vec3(1f, t, 0f), Vec3(-1f, -t, 0f), Vec3(1f, -t, 0f),
            Vec3(0f, -1f, t), Vec3(0f, 1f, t), Vec3(0f, -1f, -t), Vec3(0f, 1f, -t),
            Vec3(t, 0f, -1f), Vec3(t, 0f, 1f), Vec3(-t, 0f, -1f), Vec3(-t, 0f, 1f),
        ).map { it.normalized() }.toMutableList()
        var faces = intArrayOf(
            0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11, 1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
            3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9, 4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
        )
        repeat(subdivisions) {
            val cache = HashMap<Long, Int>()
            fun mid(a: Int, b: Int): Int {
                val key = if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
                return cache.getOrPut(key) { verts.add(((verts[a] + verts[b]) * 0.5f).normalized()); verts.size - 1 }
            }
            val next = IntArray(faces.size * 4); var n = 0
            for (f in faces.indices step 3) {
                val a = faces[f]; val b = faces[f + 1]; val c = faces[f + 2]
                val ab = mid(a, b); val bc = mid(b, c); val ca = mid(c, a)
                intArrayOf(a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca).forEach { next[n++] = it }
            }
            faces = next
        }
        return verts to faces
    }
}
