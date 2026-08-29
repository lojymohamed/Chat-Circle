package com.example.chatcircle.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.chatcircle.R
import com.example.chatcircle.domain.model.User
import kotlin.math.min

private const val TAG = "CC_InitialsAvatar"

/**
 * The app-wide fallback for a user with no photo: their first letter on a
 * coloured circle.
 *
 * This replaces the grey silhouette, which made everyone look identical in a
 * list. A letter plus a colour gives two things to recognise someone by before
 * reading the name at all.
 *
 * The drawable paints itself rather than being a vector asset because both the
 * letter and the colour vary per user - a static resource cannot do that, and
 * generating one bitmap per user would mean caching them.
 */
class InitialsAvatarDrawable(
    private val initial: String,
    backgroundColor: Int,
    textColor: Int
) : Drawable() {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas) {
        val area = bounds
        if (area.isEmpty) return

        // Inset slightly. Drawn at the full half-extent the circle sits exactly
        // on the bounds, so the host ShapeableImageView mask and its stroke
        // shave the antialiased edge - which reads as a flat-sided circle.
        val radius = (min(area.width(), area.height()) / 2f) * (1f - EDGE_INSET)
        canvas.drawCircle(area.exactCenterX(), area.exactCenterY(), radius, backgroundPaint)

        // Sized from the radius so one drawable looks right at 46dp in a list
        // and at 88dp on the profile header.
        textPaint.textSize = radius * TEXT_SCALE

        // drawText places the baseline, not the centre, so the glyph has to be
        // shifted by half its vertical extent to sit optically centred.
        val metrics = textPaint.fontMetrics
        val baseline = area.exactCenterY() - (metrics.ascent + metrics.descent) / 2f

        canvas.drawText(initial, area.exactCenterX(), baseline, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint.alpha = alpha
        textPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        backgroundPaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Drawable, but still abstract so it must be overridden")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        /** Letter height relative to the circle radius. */
        const val TEXT_SCALE = 0.95f

        /** Pulls the circle in off its own bounds so the edge is not clipped. */
        const val EDGE_INSET = 0.04f
    }
}

/**
 * Builds [InitialsAvatarDrawable]s. Use these rather than the drawable
 * constructor so every avatar in the app picks its colour the same way.
 */
object InitialsAvatar {

    private val PALETTE = intArrayOf(
        R.color.avatar_1, R.color.avatar_2, R.color.avatar_3, R.color.avatar_4,
        R.color.avatar_5, R.color.avatar_6, R.color.avatar_7, R.color.avatar_8
    )

    /** Fallback avatar for [user], keyed on their uid so it never changes. */
    fun forUser(context: Context, user: User): Drawable = of(
        context = context,
        label = user.displayName.ifBlank { user.email },
        seed = user.uid.ifBlank { user.email }
    )

    /**
     * Fallback avatar for an arbitrary [label].
     *
     * [seed] picks the colour and should be something stable like a uid - using
     * the display name instead would recolour someone the moment they rename
     * themselves.
     */
    fun of(context: Context, label: String?, seed: String?): Drawable {
        val initial = initialOf(label)
        val colorSeed = seed?.takeIf { it.isNotBlank() } ?: label.orEmpty()

        // hashCode can be negative and Int.MIN_VALUE has no positive
        // counterpart, so mask to 32 unsigned bits before taking a remainder.
        val index = ((colorSeed.hashCode().toLong() and 0xFFFFFFFFL) % PALETTE.size).toInt()

        Log.d(TAG, "of() called: initial=$initial, paletteIndex=$index")

        return InitialsAvatarDrawable(
            initial = initial,
            backgroundColor = ContextCompat.getColor(context, PALETTE[index]),
            textColor = ContextCompat.getColor(context, R.color.white)
        )
    }

    /**
     * First visible character of [label], uppercased.
     *
     * Takes a whole code point rather than a Char so names starting with an
     * emoji or a character outside the basic plane are not cut in half.
     */
    private fun initialOf(label: String?): String {
        val trimmed = label?.trim().orEmpty()
        if (trimmed.isEmpty()) return "?"

        val codePoint = trimmed.codePointAt(0)
        return String(Character.toChars(codePoint)).uppercase()
    }
}
