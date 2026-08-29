package com.example.chatcircle.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Choreographer
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withSave
import com.example.chatcircle.R
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

private const val TAG = "CC_StickerField"

/**
 * A 2D rigid-body simulation: sticker images drift around the view, bounce off
 * the walls, and collide with each other.
 *
 * Each sticker is treated as a circle with a position, velocity and mass. Every
 * frame does three things:
 *
 *   1. Integrate - move each body by its velocity, then bleed off a little
 *      energy (damping).
 *   2. Wall collisions - reflect the velocity about the wall normal, which for
 *      an axis-aligned wall is just negating one component.
 *   3. Body collisions - separate any overlapping pair, then exchange momentum
 *      along the line joining their centres.
 *
 * Two details in [stepBodies] are what make this look physical rather than
 * broken, and both are easy to leave out:
 *
 *   * Positional correction. Overlapping bodies must be pushed apart BEFORE
 *     the impulse is applied, otherwise they sink into each other and clump.
 *   * The closing-velocity guard. An impulse is only applied when the pair is
 *     actually moving together. Without that check, a pair that is already
 *     separating gets pulled back and the whole field vibrates.
 *
 * Collision is brute force over every pair, which is O(n squared). That is
 * completely fine for the handful of stickers here; a few hundred bodies would
 * want a spatial hash to skip distant pairs.
 *
 * Note on logging: [stepBodies] and [onDraw] run up to 120 times a second and
 * are deliberately not logged. Everything else follows the usual convention.
 */
class StickerFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Body(
        val bitmap: Bitmap,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        /** Collision radius. The stickers are not circles, so this is a fit. */
        val radius: Float,
        /** Proportional to area, so a big sticker shrugs off a small one. */
        val mass: Float,
        var angle: Float,
        var spin: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    private var stickerResources: List<Int> = DEFAULT_STICKERS
    private val bodies = mutableListOf<Body>()

    private var running = false
    private var lastFrameNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return

            // Scale the step by real elapsed time so the field moves at the
            // same speed on 60, 90 and 120 Hz displays. Clamped so a dropped
            // frame cannot teleport a body through a wall.
            val deltaFrames = if (lastFrameNanos == 0L) {
                1f
            } else {
                ((frameTimeNanos - lastFrameNanos) / NANOS_PER_FRAME_60HZ)
                    .coerceIn(MIN_STEP, MAX_STEP)
            }
            lastFrameNanos = frameTimeNanos

            stepBodies(deltaFrames)
            invalidate()

            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Replaces the stickers in the field. Safe to call before layout. */
    fun setStickers(@DrawableRes stickers: List<Int>) {
        Log.d(TAG, "setStickers() called: count=${stickers.size}")
        stickerResources = stickers
        if (width > 0 && height > 0) {
            buildBodies()
        }
    }

    /** Starts the simulation. Idempotent. */
    fun start() {
        Log.d(TAG, "start() called: alreadyRunning=$running, bodies=${bodies.size}")
        if (running) return

        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    /** Stops the simulation. Bodies keep their positions for the next start. */
    fun stop() {
        Log.d(TAG, "stop() called: wasRunning=$running")
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    /**
     * Shoves every body in a random direction.
     *
     * The kick is added to the existing velocity rather than replacing it, so
     * repeated taps build the field up instead of resetting it each time.
     */
    fun kick() {
        Log.d(TAG, "kick() called: bodies=${bodies.size}")

        val strength = KICK_DP.dp()
        for (body in bodies) {
            body.vx += (Random.nextFloat() - 0.5f) * strength
            body.vy += (Random.nextFloat() - 0.5f) * strength
            body.spin += (Random.nextFloat() - 0.5f) * KICK_SPIN
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged() called: ${w}x$h")

        if (w > 0 && h > 0) {
            buildBodies()
        }
    }

    /**
     * Loads each sticker and drops it somewhere random with a random velocity.
     *
     * Every sticker is scaled to the same AREA rather than the same width, so
     * a wide pill and a squat badge carry equal visual weight - matching on
     * width would make the pill tower over everything else.
     */
    private fun buildBodies() {
        Log.d(TAG, "buildBodies() called: stickers=${stickerResources.size}, size=${width}x$height")

        bodies.clear()

        val targetArea = BASE_SIZE_DP.dp() * BASE_SIZE_DP.dp()
        val startSpeed = START_SPEED_DP.dp()

        for (resourceId in stickerResources) {
            val drawable = AppCompatResources.getDrawable(context, resourceId)
            if (drawable == null) {
                Log.w(TAG, "buildBodies(): could not load drawable $resourceId, skipping")
                continue
            }

            val source = drawable.toBitmap()
            val scale = sqrt(targetArea / (source.width.toFloat() * source.height.toFloat()))
            val scaled = Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )

            // Geometric mean of the two half-extents: a circle through the
            // corners would collide far too early on the wide stickers, and one
            // inscribed in the short side would let them visibly overlap.
            val radius = sqrt(scaled.width * scaled.height.toFloat()) / 2f * RADIUS_FIT

            bodies += Body(
                bitmap = scaled,
                x = radius + Random.nextFloat() * (width - 2 * radius).coerceAtLeast(1f),
                y = radius + Random.nextFloat() * (height - 2 * radius).coerceAtLeast(1f),
                vx = (Random.nextFloat() - 0.5f) * startSpeed,
                vy = (Random.nextFloat() - 0.5f) * startSpeed,
                radius = radius,
                mass = radius * radius,
                angle = Random.nextFloat() * 360f,
                spin = (Random.nextFloat() - 0.5f) * START_SPIN
            )
        }

        Log.d(TAG, "buildBodies() success: bodies=${bodies.size}")
    }

    private fun stepBodies(deltaFrames: Float) {
        val minSpeed = MIN_SPEED_DP.dp()
        val maxSpeed = MAX_SPEED_DP.dp()
        val damping = 1f - (DRAG * deltaFrames)

        // --- 1. Integrate, damp, and bounce off the walls.
        for (body in bodies) {
            body.x += body.vx * deltaFrames
            body.y += body.vy * deltaFrames
            body.angle += body.spin * deltaFrames

            body.vx *= damping
            body.vy *= damping

            if (body.x < body.radius) {
                body.x = body.radius
                body.vx = -body.vx * RESTITUTION
            }
            if (body.x > width - body.radius) {
                body.x = width - body.radius
                body.vx = -body.vx * RESTITUTION
            }
            if (body.y < body.radius) {
                body.y = body.radius
                body.vy = -body.vy * RESTITUTION
            }
            if (body.y > height - body.radius) {
                body.y = height - body.radius
                body.vy = -body.vy * RESTITUTION
            }

            // Keep them swimming. Damping and inelastic bounces would otherwise
            // bring the field to a halt after a minute or so; this floors the
            // speed without ever adding a visible jolt.
            val speed = hypot(body.vx, body.vy)
            when {
                speed < MIN_SPEED_EPSILON -> {
                    val direction = Random.nextFloat() * TWO_PI
                    body.vx = kotlin.math.cos(direction) * minSpeed
                    body.vy = kotlin.math.sin(direction) * minSpeed
                }

                speed < minSpeed -> {
                    val boost = minSpeed / speed
                    body.vx *= boost
                    body.vy *= boost
                }

                speed > maxSpeed -> {
                    val brake = maxSpeed / speed
                    body.vx *= brake
                    body.vy *= brake
                }
            }

            body.spin = body.spin.coerceIn(-MAX_SPIN, MAX_SPIN)
        }

        // --- 2. Body against body.
        for (a in bodies.indices) {
            for (b in a + 1 until bodies.size) {
                val first = bodies[a]
                val second = bodies[b]

                val dx = second.x - first.x
                val dy = second.y - first.y
                val distance = hypot(dx, dy).coerceAtLeast(0.0001f)
                val minDistance = first.radius + second.radius

                if (distance >= minDistance) continue

                val nx = dx / distance
                val ny = dy / distance

                // (a) Separate them first, or they sink into each other.
                val overlap = (minDistance - distance) / 2f
                first.x -= nx * overlap
                first.y -= ny * overlap
                second.x += nx * overlap
                second.y += ny * overlap

                // (b) Only exchange momentum if they are closing in. Skipping
                // this check makes an already-separating pair snap back
                // together and the field buzzes.
                val relativeVx = second.vx - first.vx
                val relativeVy = second.vy - first.vy
                val closingSpeed = relativeVx * nx + relativeVy * ny
                if (closingSpeed >= 0f) continue

                val impulse = -(1f + RESTITUTION) * closingSpeed /
                        (1f / first.mass + 1f / second.mass)

                first.vx -= impulse * nx / first.mass
                first.vy -= impulse * ny / first.mass
                second.vx += impulse * nx / second.mass
                second.vy += impulse * ny / second.mass

                // A glancing hit should set them spinning.
                val spinTransfer = closingSpeed * SPIN_FROM_IMPACT
                first.spin -= spinTransfer
                second.spin += spinTransfer
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (body in bodies) {
            canvas.withSave {
                translate(body.x, body.y)
                rotate(body.angle)
                drawBitmap(
                    body.bitmap,
                    -body.bitmap.width / 2f,
                    -body.bitmap.height / 2f,
                    paint
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "onDetachedFromWindow() called")
        stop()
        super.onDetachedFromWindow()
    }

    private fun Float.dp(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        resources.displayMetrics
    )

    companion object {
        /** The Kolibri sticker set, in no particular order. */
        val DEFAULT_STICKERS = listOf(
            R.drawable.sticker_kolibri,
            R.drawable.sticker_rocket,
            R.drawable.sticker_laptop,
            R.drawable.sticker_ai_agents,
            R.drawable.sticker_nodes
        )

        /** Bounciness. 1 would bounce forever, 0 would stop dead on contact. */
        private const val RESTITUTION = 0.88f

        /** Velocity lost per 60 Hz frame. Small - the field should coast. */
        private const val DRAG = 0.0016f

        /** Every sticker is scaled to roughly this square of area. */
        private const val BASE_SIZE_DP = 78f

        /** Shrinks the collision circle so contact looks right on odd shapes. */
        private const val RADIUS_FIT = 0.92f

        private const val START_SPEED_DP = 2.2f
        private const val MIN_SPEED_DP = 0.32f
        private const val MAX_SPEED_DP = 7.5f
        private const val KICK_DP = 9f

        private const val START_SPIN = 0.35f
        private const val MAX_SPIN = 1.6f
        private const val KICK_SPIN = 1.2f
        private const val SPIN_FROM_IMPACT = 0.012f

        /** Below this a body counts as stopped and gets a fresh direction. */
        private const val MIN_SPEED_EPSILON = 0.01f

        private const val NANOS_PER_FRAME_60HZ = 16_666_666f
        private const val MIN_STEP = 0.5f
        private const val MAX_STEP = 2.5f

        private const val TWO_PI = (Math.PI * 2).toFloat()
    }
}
