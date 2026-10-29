package com.thumbbreak.thumbbreak

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * Small "7 / 20" pill drawn over Reels/Shorts. Uses an accessibility overlay window,
 * so it needs no "Display over other apps" permission. It ignores touches, so taps
 * and swipes go through to the app underneath.
 */
class CounterBadge(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var view: TextView? = null

    fun show(count: Int, limit: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return
        val badge = view ?: create().also { view = it }
        badge.text = "$count / $limit"
        (badge.background as GradientDrawable).setColor(
            if (limit - count <= WARN_REMAINING) WARN_COLOR else NORMAL_COLOR
        )
    }

    fun hide() {
        view?.let { windowManager.removeViewImmediate(it) }
        view = null
    }

    private fun create(): TextView {
        val badge = TextView(service).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(96)
        }
        windowManager.addView(badge, params)
        return badge
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), service.resources.displayMetrics
    ).toInt()

    companion object {
        private const val WARN_REMAINING = 2
        private val NORMAL_COLOR = Color.argb(150, 0, 0, 0)
        private val WARN_COLOR = Color.argb(220, 229, 57, 53)
    }
}
