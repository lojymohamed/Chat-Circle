package com.example.chatcircle.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import com.example.chatcircle.R
import kotlin.math.max

private const val TAG = "CC_BrandIntro"

/**
 * Plays the brand intro (res/raw/brand_intro.mp4) as a full-screen overlay on
 * top of MainActivity, then fades itself away.
 *
 * The controller has two jobs, and the second one is the reason this class
 * exists at all:
 *
 *  1. Play the video and fade out when it ends.
 *  2. Tell the caller the exact moment the first frame is on screen
 *     ([onFirstFrame]). The caller uses that to hold the Android system splash
 *     screen open until then. Without it the system splash disappears as soon
 *     as the Activity draws, leaving a gap while the decoder warms up - and
 *     that gap is what users read as "two splash screens".
 *
 * The intro is never allowed to block the app. Every failure path - decode
 * error, missing codec, a device that never reports a first frame - falls
 * through to [finish] via a watchdog, so the worst case is the user goes
 * straight to the app without the intro.
 *
 * Not thread safe; construct and call from the main thread only.
 */
class BrandIntroController(
    private val context: Context,
    private val overlay: View,
    private val textureView: TextureView,
    private val onFirstFrame: () -> Unit,
    private val onFinished: () -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null

    /** Guards [onFirstFrame] so it fires exactly once. */
    private var firstFrameReported = false

    /** Guards [finish] so the overlay is only torn down once. */
    private var finished = false

    private val watchdog = Runnable {
        Log.w(TAG, "watchdog fired: intro overran its budget, dismissing")
        finish()
    }

    private val firstFrameWatchdog = Runnable {
        Log.w(TAG, "firstFrameWatchdog fired: no first frame, releasing splash hold")
        reportFirstFrame()
    }

    /**
     * Begins playback. Safe to call once per instance.
     *
     * The surface may not exist yet, so playback actually starts from
     * [surfaceListener].
     */
    fun start() {
        Log.d(TAG, "start() called")

        // Never let the system splash hang if the device never reports a frame.
        handler.postDelayed(firstFrameWatchdog, FIRST_FRAME_TIMEOUT_MS)

        val existing = textureView.surfaceTexture
        if (textureView.isAvailable && existing != null) {
            Log.d(TAG, "start(): surface already available")
            openPlayer(existing)
        } else {
            Log.d(TAG, "start(): waiting for surface")
            textureView.surfaceTextureListener = surfaceListener
        }

        overlay.setOnClickListener {
            Log.i(TAG, "start(): intro skipped by tap")
            finish()
        }
    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable() called: width=$width, height=$height")
            openPlayer(st)
        }

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureSizeChanged() called: width=$width, height=$height")
            mediaPlayer?.let { applyCenterCrop(it.videoWidth, it.videoHeight) }
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            Log.d(TAG, "onSurfaceTextureDestroyed() called")
            return true
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
    }

    /**
     * Creates the MediaPlayer and prepares it asynchronously.
     *
     * MediaPlayer rather than ExoPlayer on purpose: this is a single bundled
     * MP4 with no audio track, no streaming and no DRM, which is exactly
     * MediaPlayer's happy path. Media3 would add roughly 1.5 MB to a chat app
     * for one 3.3-second animation.
     */
    private fun openPlayer(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "openPlayer() called")
        if (mediaPlayer != null) {
            Log.w(TAG, "openPlayer(): player already exists, ignoring")
            return
        }

        try {
            val uri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.brand_intro)
            val newSurface = Surface(surfaceTexture)
            surface = newSurface

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setSurface(newSurface)
                isLooping = false

                setOnPreparedListener { player -> onPrepared(player) }
                setOnInfoListener { _, what, extra -> onInfo(what, extra) }
                setOnCompletionListener { onCompletion() }
                setOnErrorListener { _, what, extra -> onError(what, extra) }

                prepareAsync()
            }
            Log.d(TAG, "openPlayer() success: prepareAsync issued")
        } catch (e: Exception) {
            Log.e(TAG, "openPlayer() failed, skipping intro", e)
            reportFirstFrame()
            finish()
        }
    }

    private fun onPrepared(player: MediaPlayer) {
        Log.d(TAG, "onPrepared() called: size=" + player.videoWidth + "x" + player.videoHeight +
                ", duration=" + player.duration + "ms")

        applyCenterCrop(player.videoWidth, player.videoHeight)

        // Absolute upper bound on how long the intro may hold the user.
        handler.postDelayed(watchdog, player.duration + WATCHDOG_GRACE_MS)

        try {
            player.start()
            Log.d(TAG, "onPrepared() success: playback started")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "onPrepared() failed to start playback", e)
            finish()
        }
    }

    private fun onInfo(what: Int, extra: Int): Boolean {
        Log.d(TAG, "onInfo() called: what=$what, extra=$extra")
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            Log.i(TAG, "onInfo(): first frame rendered")
            reportFirstFrame()
        }
        return false
    }

    private fun onCompletion() {
        Log.i(TAG, "onCompletion() called: intro finished")
        finish()
    }

    private fun onError(what: Int, extra: Int): Boolean {
        Log.e(TAG, "onError() called: what=$what, extra=$extra - skipping intro")
        reportFirstFrame()
        finish()
        return true
    }

    /**
     * Scales the video to fill the view, cropping the overflow.
     *
     * brand_intro.mp4 is already baked as a 1080x2340 vertical canvas, so on a
     * typical phone this is close to a 1:1 fill and almost nothing is cropped.
     * The logo card inside the video is fitted to width with matching blue
     * margins, so the wordmark stays intact whatever the screen ratio.
     */
    private fun applyCenterCrop(videoWidth: Int, videoHeight: Int) {
        Log.d(TAG, "applyCenterCrop() called: video=" + videoWidth + "x" + videoHeight)

        val viewWidth = textureView.width.toFloat()
        val viewHeight = textureView.height.toFloat()
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            Log.w(TAG, "applyCenterCrop(): not measured yet, skipping")
            return
        }

        val scale = max(viewWidth / videoWidth, viewHeight / videoHeight)
        // TextureView stretches the video to fill the view by default, so these
        // factors are relative to that already-stretched image.
        val scaleX = (videoWidth * scale) / viewWidth
        val scaleY = (videoHeight * scale) / viewHeight

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        textureView.setTransform(matrix)

        Log.d(TAG, "applyCenterCrop() success: scaleX=$scaleX, scaleY=$scaleY")
    }

    private fun reportFirstFrame() {
        Log.d(TAG, "reportFirstFrame() called: alreadyReported=$firstFrameReported")
        if (firstFrameReported) return
        firstFrameReported = true
        handler.removeCallbacks(firstFrameWatchdog)
        onFirstFrame()
    }

    /** Fades the overlay out, releases the player, and notifies the caller. */
    private fun finish() {
        Log.d(TAG, "finish() called: alreadyFinished=$finished")
        if (finished) return
        finished = true

        handler.removeCallbacks(watchdog)
        // If we are tearing down before a frame ever arrived, the system splash
        // is still being held - let it go or the app would hang on it.
        reportFirstFrame()

        overlay.animate()
            .alpha(0f)
            .setDuration(OVERLAY_FADE_OUT_MS)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Log.d(TAG, "finish(): fade-out complete, overlay hidden")
                    overlay.visibility = View.GONE
                    release()
                    onFinished()
                }
            })
            .start()
    }

    /** Releases the MediaPlayer and Surface. Idempotent. */
    fun release() {
        Log.d(TAG, "release() called")
        handler.removeCallbacks(watchdog)
        handler.removeCallbacks(firstFrameWatchdog)

        try {
            mediaPlayer?.apply {
                setOnPreparedListener(null)
                setOnInfoListener(null)
                setOnCompletionListener(null)
                setOnErrorListener(null)
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "release() failed to release player", e)
        }
        mediaPlayer = null

        surface?.release()
        surface = null
        Log.d(TAG, "release() success")
    }

    private companion object {
        /**
         * How long the system splash may be held waiting for a first frame.
         * Past this we give up and let the app through - a missing intro is
         * always better than a stuck launcher icon.
         */
        const val FIRST_FRAME_TIMEOUT_MS = 1_500L

        /** Slack on top of the video's own duration before force-dismissing. */
        const val WATCHDOG_GRACE_MS = 1_500L

        /** Crossfade from the last video frame into the app. */
        const val OVERLAY_FADE_OUT_MS = 250L
    }
}
