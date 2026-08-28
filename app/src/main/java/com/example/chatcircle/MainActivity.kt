package com.example.chatcircle

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.chatcircle.databinding.ActivityMainBinding
import com.example.chatcircle.ui.splash.BrandIntroController
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "CC_MainActivity"

/**
 * The single Activity. Also owns the launch experience.
 *
 * There is intentionally no splash Activity and no splash destination in the
 * nav graph. On Android 12+ the system always draws its own splash screen
 * first, so a second one would be a visible double-splash. Instead the launch
 * sequence is stitched together inside this one Activity:
 *
 *   1. The system splash paints flat white with the Konecta wordmark
 *      (see Theme.Chatcircle.Starting).
 *   2. It is held open until the video has actually rendered its first frame,
 *      so there is no black gap while the decoder warms up.
 *   3. It crossfades into the video, which is already playing underneath.
 *   4. The video ends on the solid blue brand card and fades into the app.
 *
 * The result reads as one continuous animation rather than two screens.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var brandIntro: BrandIntroController? = null

    /**
     * Drives [androidx.core.splashscreen.SplashScreen.setKeepOnScreenCondition].
     * Stays false - keeping the system splash up - until the intro's first
     * frame is on screen, or until the intro gives up.
     */
    private var readyToDismissSystemSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate().
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val isColdStart = savedInstanceState == null
        Log.d(TAG, "onCreate() called: coldStart=$isColdStart")

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        splashScreen.setKeepOnScreenCondition { !readyToDismissSystemSplash }
        splashScreen.setOnExitAnimationListener { provider ->
            Log.d(TAG, "onCreate(): system splash exiting, crossfading into video")
            ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f).apply {
                duration = SYSTEM_SPLASH_CROSSFADE_MS
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        provider.remove()
                    }
                })
                start()
            }
        }

        if (isColdStart) {
            startBrandIntro()
        } else {
            // A rotation is not a launch - skip the intro entirely.
            Log.d(TAG, "onCreate(): warm start, skipping brand intro")
            skipBrandIntro()
        }
    }

    /** Builds and starts the brand intro overlay. Cold start only. */
    private fun startBrandIntro() {
        Log.d(TAG, "startBrandIntro() called")

        brandIntro = BrandIntroController(
            context = this,
            overlay = binding.splashOverlay,
            textureView = binding.splashVideo,
            onFirstFrame = {
                Log.d(TAG, "startBrandIntro(): first frame ready, releasing system splash")
                readyToDismissSystemSplash = true
            },
            onFinished = {
                Log.i(TAG, "startBrandIntro(): intro complete, app is visible")
                brandIntro = null
            }
        ).also { it.start() }
    }

    /**
     * Lets the app draw behind the status and navigation bars.
     *
     * The theme already paints both bars transparent; this call is what stops
     * the window from reserving space for them, so a screen background runs the
     * full height of the display instead of stopping below a coloured strip.
     *
     * Screens then claim their own safe area. Most use
     * android:fitsSystemWindows="true" on their root, which turns the insets
     * into padding while the background still paints edge to edge. Onboarding
     * splits them by hand instead, so its white action sheet can extend
     * underneath the gesture bar - see OnboardingFragment.applyWindowInsets().
     *
     * Light bar icons are set here rather than in the theme because
     * android:windowLightNavigationBar needs API 27 and this app supports 24 -
     * the insets controller does the version check for us.
     */
    private fun enableEdgeToEdge() {
        Log.d(TAG, "enableEdgeToEdge() called")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Dark icons and a dark gesture pill, because the screens they sit over
        // are light: white sheets and pale backgrounds.
        //
        // Obtained via WindowCompat rather than by constructing
        // WindowInsetsControllerCompat directly - that constructor is
        // deprecated and does not reliably apply the appearance flags.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        Log.d(TAG, "enableEdgeToEdge() success: drawing behind system bars")
    }

    /** Hides the overlay immediately, without playing anything. */
    private fun skipBrandIntro() {
        Log.d(TAG, "skipBrandIntro() called")
        binding.splashOverlay.visibility = View.GONE
        readyToDismissSystemSplash = true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        brandIntro?.release()
        brandIntro = null
        super.onDestroy()
    }

    private companion object {
        /**
         * Crossfade from the system splash into the video. Short on purpose -
         * long enough to hide any colour mismatch, short enough not to read as
         * a transition of its own.
         */
        const val SYSTEM_SPLASH_CROSSFADE_MS = 180L
    }
}
