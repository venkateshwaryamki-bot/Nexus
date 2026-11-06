package com.nexus.world.gles

import android.content.res.AssetManager
import android.opengl.GLES30.*
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

internal object GlUtil {
    private const val TAG = "NexusGL"

    fun program(assets: AssetManager, vs: String, fs: String): Int {
        fun compile(type: Int, name: String): Int {
            val src = assets.open("shaders/$name").bufferedReader().use { it.readText() }
            val s = glCreateShader(type)
            glShaderSource(s, src); glCompileShader(s)
            val ok = IntArray(1); glGetShaderiv(s, GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) { val log = glGetShaderInfoLog(s); glDeleteShader(s); throw IllegalStateException("$name: $log") }
            return s
        }
        val p = glCreateProgram()
        val v = compile(GL_VERTEX_SHADER, vs); val f = compile(GL_FRAGMENT_SHADER, fs)
        glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p)
        glDeleteShader(v); glDeleteShader(f)
        val ok = IntArray(1); glGetProgramiv(p, GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) throw IllegalStateException("link $vs/$fs: ${glGetProgramInfoLog(p)}")
        return p
    }

    fun floatBuffer(a: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(a).also { it.position(0) }

    fun intBuffer(a: IntArray): IntBuffer =
        ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer().put(a).also { it.position(0) }

    fun checkError(where: String) {
        val e = glGetError()
        if (e != GL_NO_ERROR) Log.w(TAG, "GL error 0x${Integer.toHexString(e)} at $where")
    }
}

/** A VAO + buffers. [count] is indices for indexed meshes, vertices otherwise. */
internal class GpuBuffer(val vao: Int, private val buffers: IntArray, val count: Int, val indexed: Boolean) {
    fun release() {
        glDeleteVertexArrays(1, intArrayOf(vao), 0)
        glDeleteBuffers(buffers.size, buffers, 0)
    }

    companion object {
        /** Interleaved [com.nexus.world.scene.Mesh] (10 floats/vertex). */
        fun mesh(vertices: FloatArray, indices: IntArray): GpuBuffer? {
            if (indices.isEmpty()) return null
            val vao = IntArray(1); glGenVertexArrays(1, vao, 0); glBindVertexArray(vao[0])
            val b = IntArray(2); glGenBuffers(2, b, 0)
            glBindBuffer(GL_ARRAY_BUFFER, b[0])
            glBufferData(GL_ARRAY_BUFFER, vertices.size * 4, GlUtil.floatBuffer(vertices), GL_STATIC_DRAW)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, b[1])
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, GlUtil.intBuffer(indices), GL_STATIC_DRAW)
            val stride = 40
            glEnableVertexAttribArray(0); glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0)
            glEnableVertexAttribArray(1); glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 12)
            glEnableVertexAttribArray(2); glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 24)
            glEnableVertexAttribArray(3); glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 36)
            glBindVertexArray(0)
            return GpuBuffer(vao[0], b, indices.size, true)
        }

        /** Non-indexed float stream with attribute layout (location, size, offsetInFloats). */
        fun stream(data: FloatArray, strideFloats: Int, vararg layout: Triple<Int, Int, Int>): GpuBuffer? {
            if (data.isEmpty()) return null
            val vao = IntArray(1); glGenVertexArrays(1, vao, 0); glBindVertexArray(vao[0])
            val b = IntArray(1); glGenBuffers(1, b, 0)
            glBindBuffer(GL_ARRAY_BUFFER, b[0])
            glBufferData(GL_ARRAY_BUFFER, data.size * 4, GlUtil.floatBuffer(data), GL_STATIC_DRAW)
            for ((loc, size, off) in layout) {
                glEnableVertexAttribArray(loc)
                glVertexAttribPointer(loc, size, GL_FLOAT, false, strideFloats * 4, off * 4)
            }
            glBindVertexArray(0)
            return GpuBuffer(vao[0], b, data.size / strideFloats, false)
        }
    }
}
