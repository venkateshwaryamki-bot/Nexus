package com.nexus.world.gles

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.nexus.world.scene.GraphicsQuality
import com.nexus.world.scene.PlanetScene
import com.nexus.world.scene.WorldHost
import com.nexus.world.scene.WorldState
import java.util.concurrent.Executors

/**
 * Native [WorldHost] backed by OpenGL ES 3.0.
 *
 * Battery/performance design:
 * - RENDERMODE_WHEN_DIRTY + Choreographer pacing: frames are only produced while the view is
 *   resumed, and capped at [GraphicsQuality.maxFps] (30 on LOW).
 * - The surface is rendered at [GraphicsQuality.renderScale] of native resolution and upscaled by
 *   the compositor, which is the largest single win for fill-rate-bound mid-range GPUs.
 * - Scene geometry is rebuilt on a background thread only when the world state changes.
 */
@SuppressLint("ViewConstructor")
class NexusWorldView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    GLSurfaceView(context, attrs), WorldHost {

    private val renderer = NexusRenderer(context.assets)
    private val builder = Executors.newSingleThreadExecutor { r -> Thread(r, "nexus-scene-builder").apply { isDaemon = true } }
    private var quality = GraphicsQuality.MEDIUM
    private var state: WorldState? = null
    private var resumed = false
    private var lastFrameNanos = 0L
    private var buildGeneration = 0

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!resumed) return
            val minInterval = 1_000_000_000L / quality.maxFps - 2_000_000L
            if (frameTimeNanos - lastFrameNanos >= minInterval) { lastFrameNanos = frameTimeNanos; requestRender() }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun setWorldState(state: WorldState) {
        if (state == this.state) return
        this.state = state
        rebuild()
    }

    override fun setQuality(quality: GraphicsQuality) {
        if (quality == this.quality) return
        this.quality = quality
        applyRenderScale(width, height)
        rebuild()
    }

    override fun playEnergyPulse(amount: Int) = queueEvent { renderer.triggerPulse(amount) }

    private fun rebuild() {
        val s = state ?: return
        val q = quality
        val gen = ++buildGeneration
        builder.execute {
            val scene = PlanetScene.build(s, q)
            post { if (gen == buildGeneration) renderer.submitScene(scene) }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyRenderScale(w, h)
    }

    private fun applyRenderScale(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (quality.renderScale >= 0.999f) holder.setSizeFromLayout()
        else holder.setFixedSize((w * quality.renderScale).toInt(), (h * quality.renderScale).toInt())
    }

    override fun onHostResume() {
        if (resumed) return
        resumed = true
        onResume()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onHostPause() {
        if (!resumed) return
        resumed = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        onPause()
    }

    override fun onDetachedFromWindow() {
        onHostPause()
        super.onDetachedFromWindow()
    }

    // --- Touch: drag to orbit, pinch to zoom ----------------------------------------------------
    private var lastX = 0f; private var lastY = 0f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val f = d.scaleFactor; queueEvent { renderer.camera.zoom(f) }; return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = e.x; lastY = e.y; parent?.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> if (!scaleDetector.isInProgress && e.pointerCount == 1) {
                val dx = (e.x - lastX) / width; val dy = (e.y - lastY) / height
                queueEvent { renderer.camera.drag(dx, dy) }
                lastX = e.x; lastY = e.y
            }
            MotionEvent.ACTION_POINTER_UP -> { val i = if (e.actionIndex == 0) 1 else 0; lastX = e.getX(i); lastY = e.getY(i) }
        }
        return true
    }
}
