package com.example.chatcircle.ui.onboarding

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.example.chatcircle.R

/**
 * One page of the onboarding pager.
 *
 * Everything is a resource id rather than a String so the pages stay
 * translatable and so the list below can be a compile-time constant.
 *
 * The headline is split into [lead] and [emphasis] instead of being one string
 * with markup: the two halves are styled differently and the split has to
 * survive translation, which parsing a single string would not guarantee.
 */
data class OnboardingPage(
    @StringRes val lead: Int,
    @StringRes val emphasis: Int,
    @StringRes val category: Int,
    @StringRes val title: Int,
    @StringRes val meta: Int,
    /** Mock screen inflated into the phone frame. Decorative only. */
    @LayoutRes val phoneContent: Int
) {
    companion object {
        /**
         * The pages, in order.
         *
         * Keep this the same length as the number of segment views in
         * fragment_onboarding.xml - OnboardingFragment asserts on that.
         */
        val ALL: List<OnboardingPage> = listOf(
            OnboardingPage(
                lead = R.string.onboarding_1_lead,
                emphasis = R.string.onboarding_1_emphasis,
                category = R.string.onboarding_1_category,
                title = R.string.onboarding_1_title,
                meta = R.string.onboarding_1_meta,
                phoneContent = R.layout.mock_phone_chat
            ),
            OnboardingPage(
                lead = R.string.onboarding_2_lead,
                emphasis = R.string.onboarding_2_emphasis,
                category = R.string.onboarding_2_category,
                title = R.string.onboarding_2_title,
                meta = R.string.onboarding_2_meta,
                phoneContent = R.layout.mock_phone_rooms
            ),
            OnboardingPage(
                lead = R.string.onboarding_3_lead,
                emphasis = R.string.onboarding_3_emphasis,
                category = R.string.onboarding_3_category,
                title = R.string.onboarding_3_title,
                meta = R.string.onboarding_3_meta,
                phoneContent = R.layout.mock_phone_profile
            ),
            OnboardingPage(
                lead = R.string.onboarding_4_lead,
                emphasis = R.string.onboarding_4_emphasis,
                category = R.string.onboarding_4_category,
                title = R.string.onboarding_4_title,
                meta = R.string.onboarding_4_meta,
                phoneContent = R.layout.mock_phone_rooms
            ),
            OnboardingPage(
                lead = R.string.onboarding_5_lead,
                emphasis = R.string.onboarding_5_emphasis,
                category = R.string.onboarding_5_category,
                title = R.string.onboarding_5_title,
                meta = R.string.onboarding_5_meta,
                phoneContent = R.layout.mock_phone_profile
            )
        )
    }
}
