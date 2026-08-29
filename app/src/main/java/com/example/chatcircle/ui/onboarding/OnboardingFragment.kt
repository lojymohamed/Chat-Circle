package com.example.chatcircle.ui.onboarding

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.chatcircle.R
import com.example.chatcircle.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "CC_Onboarding"

/**
 * First screen of the app: a pager of feature pages over a drifting brand
 * backdrop, with story-style progress and a fixed action sheet.
 *
 * This is the nav graph start destination and it gates on the session, not on a
 * first-run flag: anyone who is not signed in sees the intro on every launch,
 * and signing in is the only thing that dismisses it. Users with an active
 * session skip straight to the room list, which happens while the brand intro
 * video still covers the window, so it is never visible.
 */
@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private val viewModel: OnboardingViewModel by viewModels()

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    /** Fills the current segment and advances the pager when it completes. */
    private var progressAnimator: ValueAnimator? = null

    /** Every backdrop animator - blobs and N outlines - kept so they can be cancelled. */
    private val blobAnimators = mutableListOf<Animator>()

    /**
     * False when the intro was skipped for a signed-in user. Guards onResume so
     * it never restarts animations for a screen that is on its way out.
     */
    private var introRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called")

        applyWindowInsets()

        if (viewModel.isSignedIn()) {
            Log.i(TAG, "onViewCreated(): session already active, skipping intro")
            goToRooms()
            return
        }

        introRunning = true
        setupPager()
        setupActions()
        startBlobDrift()
    }

    /**
     * Splits the window insets between the root and the action sheet.
     *
     * The top inset pads the root, so the backdrop still paints behind the
     * status bar while the progress bar clears it. The bottom inset pads the
     * action sheet instead of the root, so the white sheet extends underneath
     * the gesture bar rather than stopping above it and leaving a blue strip.
     */
    private fun applyWindowInsets() {
        Log.d(TAG, "applyWindowInsets() called")

        val sheetBasePadding = binding.actionSheet.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.d(TAG, "applyWindowInsets(): top=${bars.top}, bottom=${bars.bottom}")

            binding.root.updatePadding(top = bars.top)
            binding.actionSheet.updatePadding(bottom = sheetBasePadding + bars.bottom)

            windowInsets
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setupPager() {
        Log.d(TAG, "setupPager() called: pages=${OnboardingPage.ALL.size}")

        binding.onboardingPager.adapter = OnboardingPagerAdapter()

        // Pages are driven by taps and by the progress timer only - dragging
        // the pager sideways is not part of this screen.
        binding.onboardingPager.isUserInputEnabled = false

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                Log.d(TAG, "onPageSelected() called: position=$position")
                startProgress(position)
            }
        }.also { binding.onboardingPager.registerOnPageChangeCallback(it) }

        setupTapNavigation()
        startProgress(0)
    }

    /**
     * Left half goes back a page, right half goes forward.
     *
     * A GestureDetector is used rather than a bare ACTION_UP check so that a
     * press-and-drag is not counted as a tap; only a genuine single tap moves
     * the page.
     */
    private fun setupTapNavigation() {
        Log.d(TAG, "setupTapNavigation() called")

        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                // Must return true, or no later gesture callbacks arrive.
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val forward = e.x >= binding.tapOverlay.width / 2f
                    Log.d(TAG, "onSingleTapUp() called: x=${e.x}, forward=$forward")
                    goToPage(binding.onboardingPager.currentItem + if (forward) 1 else -1)
                    return true
                }
            }
        )

        binding.tapOverlay.setOnTouchListener { view, event ->
            val handled = detector.onTouchEvent(event)
            // Keeps the view accessibility-correct; the detector does the work.
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            handled
        }
    }

    /**
     * Jumps straight to [target], clamped to the available pages.
     *
     * smoothScroll is false on purpose: pages replace each other instantly,
     * with no sliding transition.
     */
    private fun goToPage(target: Int) {
        val clamped = target.coerceIn(0, OnboardingPage.ALL.lastIndex)
        val current = binding.onboardingPager.currentItem
        Log.d(TAG, "goToPage() called: target=$target, clamped=$clamped, current=$current")

        if (clamped == current) {
            Log.d(TAG, "goToPage(): already at the end of the run, ignoring")
            return
        }
        binding.onboardingPager.setCurrentItem(clamped, false)
    }

    /**
     * Runs the progress bar for [position], then advances to the next page.
     *
     * Earlier segments are snapped full and later ones empty, so this is also
     * what repaints the bar correctly when the user swipes by hand.
     *
     * The fill is driven by scaleX rather than width, so the whole animation
     * stays off the layout pass.
     */
    private fun startProgress(position: Int) {
        Log.d(TAG, "startProgress() called: position=$position")

        progressAnimator?.cancel()

        val segments = binding.progressSegments
        if (segments.childCount != OnboardingPage.ALL.size) {
            Log.w(
                TAG,
                "startProgress(): segment count ${segments.childCount} does not match " +
                        "page count ${OnboardingPage.ALL.size} - fragment_onboarding.xml needs updating"
            )
        }

        for (index in 0 until segments.childCount) {
            fillViewAt(index)?.scaleX = if (index < position) 1f else 0f
        }

        val fill = fillViewAt(position)
        if (fill == null) {
            Log.w(TAG, "startProgress(): no fill view at $position, skipping")
            return
        }

        var cancelled = false
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PAGE_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { fill.scaleX = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    advancePastPage(position)
                }
            })
            start()
        }
    }

    /** Moves to the next page, or stops on the last one with the bar full. */
    private fun advancePastPage(position: Int) {
        val lastIndex = OnboardingPage.ALL.lastIndex
        Log.d(TAG, "advancePastPage() called: position=$position, last=$lastIndex")

        if (position < lastIndex) {
            // Via goToPage so the timer advances instantly too, matching taps.
            goToPage(position + 1)
        } else {
            Log.d(TAG, "advancePastPage(): reached the last page, holding")
        }
    }

    /** The fill (second child) of the segment at [index], if present. */
    private fun fillViewAt(index: Int): View? {
        val segment = binding.progressSegments.getChildAt(index) as? ViewGroup ?: return null
        return segment.getChildAt(1)
    }

    /**
     * Gives each background shape a slow, looping drift.
     *
     * Every shape gets its own distance, duration and start delay - with
     * matching values they would visibly move as one block, which reads as a
     * sliding image rather than as depth. The durations are also deliberately
     * not multiples of each other, so the whole field takes a long time to
     * repeat a arrangement.
     */
    private fun startBlobDrift() {
        Log.d(TAG, "startBlobDrift() called")

        cancelBlobDrift()

        driftBlob(binding.blobOne, dxDp = -26f, dyDp = 34f, durationMs = 9_000L, delayMs = 0L)
        driftBlob(binding.blobTwo, dxDp = 34f, dyDp = -28f, durationMs = 11_500L, delayMs = 700L)
        driftBlob(binding.blobThree, dxDp = 30f, dyDp = -38f, durationMs = 13_300L, delayMs = 1_400L)
        driftBlob(binding.blobFour, dxDp = -38f, dyDp = 24f, durationMs = 10_700L, delayMs = 2_100L)

        // The N outlines move more slowly than the blobs and add a little
        // rotation, so they read as drifting rather than sliding. Starting
        // rotations match the values set in the layout.
        driftOutline(
            view = binding.nOutlineFaint,
            dxDp = 46f,
            dyDp = -22f,
            fromRotation = -8f,
            toRotation = -2.5f,
            durationMs = 17_000L,
            delayMs = 0L
        )
        driftOutline(
            view = binding.nOutlineAccent,
            dxDp = -40f,
            dyDp = 28f,
            fromRotation = 7f,
            toRotation = 1.5f,
            durationMs = 14_300L,
            delayMs = 900L
        )
    }

    private fun driftOutline(
        view: View,
        dxDp: Float,
        dyDp: Float,
        fromRotation: Float,
        toRotation: Float,
        durationMs: Long,
        delayMs: Long
    ) {
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, dxDp.toPx()),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, dyDp.toPx()),
            PropertyValuesHolder.ofFloat(View.ROTATION, fromRotation, toRotation)
        ).apply {
            duration = durationMs
            startDelay = delayMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        blobAnimators += animator
    }

    private fun driftBlob(
        view: View,
        dxDp: Float,
        dyDp: Float,
        durationMs: Long,
        delayMs: Long
    ) {
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, dxDp.toPx()),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, dyDp.toPx())
        ).apply {
            duration = durationMs
            startDelay = delayMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        blobAnimators += animator
    }

    private fun cancelBlobDrift() {
        Log.d(TAG, "cancelBlobDrift() called: count=${blobAnimators.size}")
        blobAnimators.forEach { it.cancel() }
        blobAnimators.clear()
    }

    private fun Float.toPx(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        resources.displayMetrics
    )

    private fun setupActions() {
        Log.d(TAG, "setupActions() called")

        binding.btnHaveAccount.setOnClickListener {
            Log.i(TAG, "setupActions(): sign-in chosen")
            goToLogin()
        }

        binding.btnCreateAccount.setOnClickListener {
            Log.i(TAG, "setupActions(): sign-up chosen")
            goToRegister()
        }
    }

    private fun goToLogin() {
        Log.d(TAG, "goToLogin() called")
        findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
    }

    /**
     * Opens registration via the login screen.
     *
     * Navigating straight to registration would leave it alone on the back
     * stack, so its own "Already have an account?" link - which just pops -
     * would close the app. Going through login first builds the back stack the
     * registration screen already expects.
     */
    private fun goToRegister() {
        Log.d(TAG, "goToRegister() called")
        findNavController().navigate(R.id.action_onboardingFragment_to_loginFragment)
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    private fun goToRooms() {
        Log.d(TAG, "goToRooms() called")
        findNavController().navigate(R.id.action_onboardingFragment_to_homeFragment)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called: introRunning=$introRunning")
        if (!introRunning) return

        startBlobDrift()
        startProgress(binding.onboardingPager.currentItem)
    }

    override fun onPause() {
        Log.d(TAG, "onPause() called")
        // Nothing should animate while the screen is not in front of anyone.
        progressAnimator?.cancel()
        cancelBlobDrift()
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() called")

        progressAnimator?.cancel()
        progressAnimator = null
        cancelBlobDrift()

        pageChangeCallback?.let { binding.onboardingPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        binding.onboardingPager.adapter = null

        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /** How long each page holds before the pager advances. */
        const val PAGE_DURATION_MS = 5_000L
    }
}
