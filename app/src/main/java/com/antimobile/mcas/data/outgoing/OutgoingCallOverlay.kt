package com.antimobile.mcas.data.outgoing

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.theme.ThemeSettings
import java.lang.ref.WeakReference

/**
 * Popup hệ thống mô phỏng đúng cấu trúc AppDialog (title/content/divider/button).
 * AppDialog Compose cần window token của Activity nên không thể tự nổi trên ứng dụng Điện thoại;
 * TYPE_APPLICATION_OVERLAY là cửa sổ phù hợp cho chế độ người dùng chủ động chọn ở đây.
 */
object OutgoingCallOverlay {
    private const val AUTO_DISMISS_MS = 9_000L
    private const val SHADOW_ELEVATION_DP = 8
    private const val SHADOW_INSET_DP = 16
    private const val HORIZONTAL_MODAL_MARGIN_DP = 24
    private val mainHandler = Handler(Looper.getMainLooper())
    private var attachedView = WeakReference<View>(null)
    private var attachedWindowManager = WeakReference<WindowManager>(null)

    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun permissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri(),
        )

    @MainThread
    internal fun dismiss() {
        check(Looper.myLooper() == Looper.getMainLooper())
        dismissCurrent()
    }

    @MainThread
    fun show(context: Context, event: OutgoingCallAlertEvent): Boolean {
        if (!canDraw(context)) return false
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "OutgoingCallOverlay.show must run on the main thread"
        }
        val appContext = context.applicationContext
        val windowManager = appContext.getSystemService(WindowManager::class.java) ?: return false
        dismissCurrent()

        val strings = appStrings().outgoingCall
        val palette = ThemeSettings.colors
        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            // Bóng thấp, đậm và sát viền để card nổi rõ mà không tạo quầng rộng.
            elevation = dp(appContext, SHADOW_ELEVATION_DP).toFloat()
            outlineAmbientShadowColor = Color.argb(102, 0, 0, 0)
            outlineSpotShadowColor = Color.argb(122, 0, 0, 0)
            background = roundedBackground(palette.cardSurface.toArgb(), dp(appContext, 20).toFloat())
        }
        card.addView(
            TextView(appContext).apply {
                text = OutgoingCallNotifier.alertTitle(event, strings)
                setTextColor(palette.textPrimary.toArgb())
                textSize = 17f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(appContext, 24), dp(appContext, 22), dp(appContext, 24), dp(appContext, 12))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        card.addView(
            TextView(appContext).apply {
                text = OutgoingCallNotifier.alertBody(event, strings)
                setTextColor(palette.textSecondary.toArgb())
                textSize = 15f
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.12f)
                setPadding(dp(appContext, 24), 0, dp(appContext, 24), dp(appContext, 18))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        card.addView(
            View(appContext).apply { setBackgroundColor(palette.dividerColor.toArgb()) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 1)),
        )
        card.addView(
            TextView(appContext).apply {
                text = strings.close
                setTextColor(palette.primary.toArgb())
                textSize = 16f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isClickable = true
                isFocusable = true
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(palette.primary.copy(alpha = 0.12f).toArgb()),
                    null,
                    null,
                )
                setOnClickListener { dismissCurrent() }
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(appContext, 54)),
        )

        val root = FrameLayout(appContext).apply {
            // Window WRAP_CONTENT cắt mọi pixel nằm ngoài root. Chừa inset lớn hơn elevation ở cả
            // bốn cạnh để bóng không bị xén, đồng thời giảm margin ngang cho modal rộng, dễ đọc hơn.
            clipChildren = false
            clipToPadding = false
            setPadding(
                dp(appContext, HORIZONTAL_MODAL_MARGIN_DP),
                dp(appContext, SHADOW_INSET_DP),
                dp(appContext, HORIZONTAL_MODAL_MARGIN_DP),
                dp(appContext, SHADOW_INSET_DP),
            )
            addView(
                card,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            // Cảnh báo phải xuất hiện ngay trong pha DIALING; animation dialog của OEM có thể trì
            // hoãn gần một giây hoặc bị bỏ khi màn hình Điện thoại đổi window liên tiếp.
            windowAnimations = 0
        }

        val added = runCatching { windowManager.addView(root, params) }.isSuccess
        if (!added) return false
        attachedView = WeakReference(root)
        attachedWindowManager = WeakReference(windowManager)
        mainHandler.postDelayed({ dismissIfCurrent(root) }, AUTO_DISMISS_MS)
        return true
    }

    @MainThread
    private fun dismissIfCurrent(view: View) {
        if (attachedView.get() === view) dismissCurrent()
    }

    @MainThread
    private fun dismissCurrent() {
        val view = attachedView.get() ?: return
        val manager = attachedWindowManager.get()
        attachedView.clear()
        attachedWindowManager.clear()
        // Synchronous removeViewImmediate can hold the app main thread behind an OEM
        // WindowManager transaction while Telecom is starting the in-call UI.
        runCatching { manager?.removeView(view) }
    }

    private fun roundedBackground(color: Int, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
