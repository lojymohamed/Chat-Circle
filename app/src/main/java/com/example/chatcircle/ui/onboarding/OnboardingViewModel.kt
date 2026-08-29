package com.example.chatcircle.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.chatcircle.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "CC_OnboardingVM"

/**
 * Decides whether the onboarding intro should run.
 *
 * The rule is deliberately simple: onboarding is shown to anyone who is not
 * signed in, every launch. It is not a one-time first-run screen, so there is
 * no "seen" flag anywhere - signing in is the only thing that dismisses it.
 *
 * This exists as a ViewModel rather than a Firebase call inside the Fragment so
 * the UI layer keeps talking only to the domain layer.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * True when a session is already active, in which case the intro is skipped
     * and the app opens straight on the room list.
     *
     * Read on demand rather than cached in init: the Fragment asks once during
     * onViewCreated, and a stale answer there would send a signed-out user into
     * the app.
     */
    fun isSignedIn(): Boolean {
        val signedIn = authRepository.currentUser() != null
        Log.d(TAG, "isSignedIn() called: signedIn=$signedIn")
        return signedIn
    }
}
