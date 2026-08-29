package com.example.chatcircle.ui.onboarding

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.chatcircle.R
import com.example.chatcircle.databinding.ItemOnboardingPageBinding

private const val TAG = "CC_OnboardAdapter"

/**
 * Renders [OnboardingPage.ALL] into the onboarding pager.
 *
 * The list is fixed at compile time, so there is no DiffUtil and no submitList -
 * a plain adapter over a constant list is all this needs.
 */
class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage> = OnboardingPage.ALL
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(
        val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Breathing animation on the floating card. Runs only while attached. */
        var pulse: Animator? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        Log.d(TAG, "onCreateViewHolder() called: viewType=$viewType")
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder() called: position=$position")

        val page = pages[position]
        val context = holder.binding.root.context

        holder.binding.tvHeadline.text = buildHeadline(
            context.getString(page.lead),
            context.getString(page.emphasis),
            ContextCompat.getColor(context, R.color.white)
        )

        holder.binding.tvCardCategory.text = context.getString(page.category)
        holder.binding.tvCardTitle.text = context.getString(page.title)
        holder.binding.tvCardMeta.text = context.getString(page.meta)

        // ViewHolders are recycled, so clear whatever mock screen was in the
        // frame before inflating this page's own.
        holder.binding.phoneScreen.removeAllViews()
        LayoutInflater.from(context).inflate(page.phoneContent, holder.binding.phoneScreen, true)

        Log.d(TAG, "onBindViewHolder() success: position=$position")
    }

    /**
     * Starts the card pulse when a page comes on screen.
     *
     * Driven from attach rather than bind because ViewPager2 binds pages before
     * they are visible; starting on bind would leave off-screen pages animating
     * for no reason.
     */
    override fun onViewAttachedToWindow(holder: PageViewHolder) {
        super.onViewAttachedToWindow(holder)
        Log.d(TAG, "onViewAttachedToWindow() called: position=${holder.bindingAdapterPosition}")

        holder.pulse?.cancel()
        holder.pulse = ObjectAnimator.ofPropertyValuesHolder(
            holder.binding.floatingCard,
            PropertyValuesHolder.ofFloat(android.view.View.SCALE_X, 1f, PULSE_SCALE),
            PropertyValuesHolder.ofFloat(android.view.View.SCALE_Y, 1f, PULSE_SCALE)
        ).apply {
            duration = PULSE_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onViewDetachedFromWindow(holder: PageViewHolder) {
        Log.d(TAG, "onViewDetachedFromWindow() called: position=${holder.bindingAdapterPosition}")

        holder.pulse?.cancel()
        holder.pulse = null
        // Leave the card at rest, or a recycled holder inherits a scaled view.
        holder.binding.floatingCard.scaleX = 1f
        holder.binding.floatingCard.scaleY = 1f

        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = pages.size

    /**
     * Builds the two-tone headline: a dimmed lead-in followed by the emphasised
     * phrase in solid white bold.
     *
     * Done with spans rather than two TextViews so the emphasis can begin
     * mid-line and wrap naturally, which is the whole look.
     *
     * [StyleSpan] is used instead of a typeface span because
     * TypefaceSpan(Typeface) needs API 28 and this app supports 24; bolding the
     * Poppins face gives the same result everywhere.
     */
    private fun buildHeadline(lead: String, emphasis: String, solidColor: Int): CharSequence {
        val dimmed = (solidColor and 0x00FFFFFF) or (DIMMED_ALPHA shl 24)

        return SpannableStringBuilder().apply {
            append(lead)
            setSpan(ForegroundColorSpan(dimmed), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            append(" ")

            val emphasisStart = length
            append(emphasis)
            setSpan(
                ForegroundColorSpan(solidColor),
                emphasisStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                emphasisStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private companion object {
        /** Alpha applied to the lead-in half of the headline (about 55%). */
        const val DIMMED_ALPHA = 0x8C

        /** Kept small - the card should breathe, not throb. */
        const val PULSE_SCALE = 1.035f
        const val PULSE_DURATION_MS = 1_150L
    }
}
