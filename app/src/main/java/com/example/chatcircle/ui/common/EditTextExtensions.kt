package com.example.chatcircle.ui.common

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

private const val TAG = "CC_EditTextExt"

/**
 * Gives this field the focused look on entry without opening the keyboard, then
 * behaves normally from the first tap onward.
 *
 * Why this is not just requestFocus(): focusing an EditText normally raises the
 * soft keyboard, which covers half the screen before the user has asked for it.
 * Setting showSoftInputOnFocus = false suppresses that, so the field shows its
 * highlighted outline and raised label over an unobstructed screen.
 *
 * The catch is that suppressing it also breaks the tap: the field already holds
 * focus, so tapping changes nothing and no keyboard would ever appear. The
 * touch listener below therefore re-enables the normal behaviour and raises the
 * keyboard by hand, then removes itself so the field is left completely
 * ordinary afterwards.
 */
fun EditText.focusWithoutKeyboard() {
    Log.d(TAG, "focusWithoutKeyboard() called")

    showSoftInputOnFocus = false
    requestFocus()

    setOnTouchListener { view, event ->
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            Log.d(TAG, "focusWithoutKeyboard(): first tap, handing back to the keyboard")

            view.performClick()
            showSoftInputOnFocus = true
            showKeyboard()

            // One-shot: from here on this is a perfectly normal field.
            setOnTouchListener(null)
        }
        // Never consume, so the tap still places the caret.
        false
    }
}

/** Asks the IME to open for this field. It must already hold focus. */
private fun EditText.showKeyboard() {
    // hasFocus() is a method on View, not a Kotlin property - the parentheses
    // are required.
    Log.d(TAG, "showKeyboard() called: hasFocus=${hasFocus()}")

    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    if (inputMethodManager == null) {
        Log.w(TAG, "showKeyboard(): no InputMethodManager available")
        return
    }

    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}
