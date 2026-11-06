package com.nexus.world.gles

import android.content.res.AssetManager
import android.opengl.GLES30.*
import android.opengl.GLSurfaceView
import android.os.SystemClock
import com.nexus.world.scene.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 3.0 renderer for the planet view. Runs entirely on the GL thread. Draw order and GL
 * state match tools/render-harness/harness.html, which verifies the shaders headlessly.
 *
 * Frame cost on the target mid-range class: ~30k triangles, 6 programs, ~10 draw calls.
 */
internal class NexusRenderer(private val assets: AssetManager) : GLSurfaceView.Renderer {
    private class Programs(val sky: Int, val lit: Int, val atmo: Int, val line: Int, val particle: Int, val glow: Int)
    private class Gpu(
        val planet: GpuBuffer?, val structures: GpuBuffer?, val orbiters: GpuBuffer?, val atmo: GpuBuffer?,
        val lines: GpuBuffer?, val orbitLines: GpuBuffer?, val particles: GpuBuffer?, val glows: GpuBuffer?,
    ) { fun release() = listOfNotNull(planet, structures, orbiters, atmo, lines, orbitLines, particles, glows).forEach { it.release() } }

    val camera = OrbitCamera()
    private var programs: Programs? = null
    private var gpu: Gpu? = null
    @Volatile private var pendingScene: SceneData? = null
    private var currentScene: SceneData? = null
    private var width = 1; private var height = 1
    private val startTime = SystemClock.elapsedRealtime()
    private var lastFrame = 0L
    private var pulseStart = -100f
    private var pulseAmount = 0
    private val uniformCache = HashMap<Long, Int>()
    private val identity = Mat4.identity()

    /** Called from any thread; the scene is uploaded on the next frame. */
    fun submitScene(scene: SceneData) { pendingScene = scene }

    /** GL thread. */
    fun triggerPulse(amount: Int) { pulseStart = now(); pulseAmount = amount }

    private fun now() = (SystemClock.elapsedRealtime() - startTime) / 1000f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Context (re)created: every GL object from a previous context is invalid.
        gpu = null; uniformCache.clear()
        programs = Programs(
            GlUtil.program(assets, "sky.vert", "sky.frag"), GlUtil.program(assets, "lit.vert", "lit.frag"),
            GlUtil.program(assets, "atmo.vert", "atmo.frag"), GlUtil.program(assets, "line.vert", "line.frag"),
            GlUtil.program(assets, "particle.vert", "particle.frag"), GlUtil.program(assets, "glow.vert", "glow.frag"),
        )
        currentScene?.let { if (pendingScene == null) pendingScene = it }
        glClearColor(0.024f, 0.075f, 0.13f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) { width = w; height = h; glViewport(0, 0, w, h) }

    private fun u(program: Int, name: String): Int =
        uniformCache.getOrPut((program.toLong() shl 32) xor name.hashCode().toLong()) { glGetUniformLocation(program, name) }

    private fun uploadIfPending() {
        val s = pendingScene ?: return
        pendingScene = null
        gpu?.release()
        gpu = Gpu(
            GpuBuffer.mesh(s.planet.vertices, s.planet.indices),
            GpuBuffer.mesh(s.structures.vertices, s.structures.indices),
            GpuBuffer.mesh(s.orbiters.vertices, s.orbiters.indices),
            GpuBuffer.mesh(s.atmosphere.vertices, s.atmosphere.indices),
            GpuBuffer.stream(s.lines, 4, Triple(0, 3, 0), Triple(1, 1, 3)),
            GpuBuffer.stream(s.orbitLines, 4, Triple(0, 3, 0), Triple(1, 1, 3)),
            GpuBuffer.stream(s.particles, 6, Triple(0, 3, 0), Triple(1, 3, 3)),
            GpuBuffer.stream(s.glows, 8, Triple(0, 4, 0), Triple(1, 4, 4)),
        )
        currentScene = s
        GlUtil.checkError("upload")
    }

    override fun onDrawFrame(gl: GL10?) {
        uploadIfPending()
        val p = programs ?: return
        val t = now()
        val dt = if (lastFrame == 0L) 0f else ((SystemClock.elapsedRealtime() - lastFrame) / 1000f).coerceAtMost(0.1f)
        lastFrame = SystemClock.elapsedRealtime()
        camera.update(dt)

        val aspect = width.toFloat() / height
        val proj = camera.projection(aspect)
        val vp = Mat4.multiply(proj, camera.view())
        val eye = camera.eye()
        val sun = RenderConstants.SunDir
        val pulse = ((t - pulseStart) / RenderConstants.PULSE_SECONDS).let { if (it in 0f..1f) it else -1f }
        val boost = if (pulse >= 0f) (1f - pulse) * (0.6f + (pulseAmount / 60f).coerceAtMost(1f) * 0.4f) else 0f
        val scene = currentScene
        val g = gpu

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // 1. Sky
        glDisable(GL_DEPTH_TEST); glDepthMask(false); glDisable(GL_BLEND)
        glUseProgram(p.sky)
        glUniformMatrix4fv(u(p.sky, "uInvViewProj"), 1, false, Mat4.invert(vp), 0)
        glUniform1f(u(p.sky, "uTime"), t); vec3(p.sky, "uSunDir", sun); glUniform1f(u(p.sky, "uStarDensity"), 1f)
        vec3(p.sky, "uNebulaA", RenderConstants.NebulaA); vec3(p.sky, "uNebulaB", RenderConstants.NebulaB)
        glBindVertexArray(0); glDrawArrays(GL_TRIANGLES, 0, 3)
        if (scene == null || g == null) return

        // 2. Opaque lit geometry
        glEnable(GL_DEPTH_TEST); glDepthMask(true); glEnable(GL_CULL_FACE); glCullFace(GL_BACK)
        glUseProgram(p.lit)
        glUniformMatrix4fv(u(p.lit, "uViewProj"), 1, false, vp, 0)
        vec3(p.lit, "uCamPos", eye); vec3(p.lit, "uSunDir", sun); glUniform1f(u(p.lit, "uTime"), t)
        glUniform1f(u(p.lit, "uDevelopment"), scene.development); glUniform1f(u(p.lit, "uPulse"), pulse)
        vec3(p.lit, "uAccent", RenderConstants.Accent); vec3(p.lit, "uPulseColor", RenderConstants.PulseColor)
        val orbitModel = Mat4.rotationY(t * RenderConstants.ORBIT_SPEED)
        drawLit(p.lit, g.planet, 0, identity)
        drawLit(p.lit, g.structures, 1, identity)
        drawLit(p.lit, g.orbiters, 1, orbitModel)

        // 3. Additive layers: energy network, atmosphere, particles, glow sprites
        glEnable(GL_BLEND); glBlendFunc(GL_ONE, GL_ONE); glDepthMask(false); glDisable(GL_CULL_FACE)
        glUseProgram(p.line)
        glUniformMatrix4fv(u(p.line, "uViewProj"), 1, false, vp, 0); glUniform1f(u(p.line, "uTime"), t)
        vec3(p.line, "uColor", RenderConstants.Accent)
        drawLines(p.line, g.lines, identity); drawLines(p.line, g.orbitLines, orbitModel)

        g.atmo?.let {
            glEnable(GL_CULL_FACE)
            glUseProgram(p.atmo)
            glUniformMatrix4fv(u(p.atmo, "uViewProj"), 1, false, vp, 0)
            vec3(p.atmo, "uCamPos", eye); vec3(p.atmo, "uSunDir", sun); vec3(p.atmo, "uAccent", RenderConstants.Accent)
            glUniform1f(u(p.atmo, "uStrength"), RenderConstants.atmosphereStrength(scene.development) + boost * 0.5f)
            glBindVertexArray(it.vao); glDrawElements(GL_TRIANGLES, it.count, GL_UNSIGNED_INT, 0)
            glDisable(GL_CULL_FACE)
        }
        g.particles?.let {
            glUseProgram(p.particle)
            glUniformMatrix4fv(u(p.particle, "uViewProj"), 1, false, vp, 0); glUniform1f(u(p.particle, "uTime"), t)
            glUniform1f(u(p.particle, "uPointScale"), height / 1170f * 2f); glUniform1f(u(p.particle, "uBoost"), boost)
            vec3(p.particle, "uColorA", RenderConstants.ParticleA); vec3(p.particle, "uColorB", RenderConstants.ParticleB)
            glBindVertexArray(it.vao); glDrawArrays(GL_POINTS, 0, it.count)
        }
        g.glows?.let {
            glUseProgram(p.glow)
            glUniformMatrix4fv(u(p.glow, "uViewProj"), 1, false, vp, 0)
            glUniformMatrix4fv(u(p.glow, "uModel"), 1, false, identity, 0)
            glUniform1f(u(p.glow, "uViewportHeight"), height.toFloat()); glUniform1f(u(p.glow, "uProjScale"), proj[5])
            glUniform1f(u(p.glow, "uTime"), t); glUniform1f(u(p.glow, "uBoost"), boost)
            glBindVertexArray(it.vao); glDrawArrays(GL_POINTS, 0, it.count)
        }
        glBindVertexArray(0)
        glDepthMask(true)
    }

    private fun drawLit(prog: Int, b: GpuBuffer?, material: Int, model: FloatArray) {
        b ?: return
        glUniform1i(u(prog, "uMaterial"), material)
        glUniformMatrix4fv(u(prog, "uModel"), 1, false, model, 0)
        glBindVertexArray(b.vao); glDrawElements(GL_TRIANGLES, b.count, GL_UNSIGNED_INT, 0)
    }

    private fun drawLines(prog: Int, b: GpuBuffer?, model: FloatArray) {
        b ?: return
        glUniformMatrix4fv(u(prog, "uModel"), 1, false, model, 0)
        glBindVertexArray(b.vao); glDrawArrays(GL_LINES, 0, b.count)
    }

    private fun vec3(prog: Int, name: String, v: Vec3) = glUniform3f(u(prog, name), v.x, v.y, v.z)
}
