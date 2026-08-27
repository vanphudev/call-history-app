package com.antimobile.callhs

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.local.CategoryCatalog
import com.antimobile.callhs.data.blocking.CallBlockNotifier
import com.antimobile.callhs.data.blocking.CallBlockNotificationSettings
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.ui.navigation.AppNav
import com.antimobile.callhs.ui.permissions.PermissionGate
import com.antimobile.callhs.i18n.LanguageSettings
import com.antimobile.callhs.ui.components.AppToastHost
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.CallHSTheme
import com.antimobile.callhs.ui.theme.ThemeSettings
import com.antimobile.callhs.util.AppMigrations
import com.antimobile.callhs.util.DeviceInfo
import com.antimobile.callhs.util.FontScaleSettings
import com.antimobile.callhs.util.SimScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * CHẶN cỡ chữ HỆ THỐNG ("Cỡ chữ" / Font size): ghim fontScale = 1.0 cho toàn Activity, BẤT KỂ người
     * dùng để cỡ chữ máy 150–200%. Nhờ đó thang chữ thiết kế không bị hệ thống phóng to làm vỡ. Cỡ chữ hiển
     * thị trong app do người dùng tự chọn, áp riêng ở tầng Compose (xem [FontScaleSettings] + [CallHSTheme]).
     *
     * LƯU Ý phạm vi: đây CHỈ chặn "Cỡ chữ", KHÔNG chặn "Cỡ hiển thị" (Display size — thay đổi densityDpi).
     * "Cỡ hiển thị" phóng ĐỀU cả dp lẫn sp nên layout vẫn cân đối, hiếm khi vỡ. Nếu muốn khoá luôn cả nó thì
     * thêm `densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE` vào cấu hình bên dưới (cân nhắc: làm vậy là
     * GHI ĐÈ một cài đặt trợ năng của người dùng).
     *
     * Sao chép nguyên cấu hình gốc rồi chỉ đổi fontScale để giữ locale, mật độ điểm ảnh, ngôn ngữ…
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { fontScale = 1.0f }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dọn dữ liệu theo bậc (reset cache / gộp mẫu tin nhắn mới) — chỉ chạy khi CẦN, đúng một lần.
        // Phải xong TRƯỚC setContent để khung hình đầu đã thấy dữ liệu đúng và modal không nhấp nháy.
        AppMigrations.run(applicationContext)
        DeviceInfo.init(applicationContext)
        // Nạp cỡ chữ đã lưu TRƯỚC setContent để khung hình đầu đã đúng cỡ, không nhấp nháy.
        FontScaleSettings.init(applicationContext)
        // Xác định NGÔN NGỮ (đã lưu hoặc theo hệ thống) TRƯỚC setContent → khung hình đầu đã đúng ngôn ngữ.
        LanguageSettings.init(applicationContext)
        // Xác định CHẾ ĐỘ Sáng/Tối (đã lưu, hoặc theo máy nếu SYSTEM) TRƯỚC setContent → khung hình đầu đúng chế độ.
        ThemeSettings.init(applicationContext)
        // Nạp cài đặt bộ chặn và tạo notification channel một lần; service vẫn đọc prefs trực tiếp
        // nếu Android khởi chạy process chỉ để screening khi Activity chưa từng mở.
        CallBlockSettings.init(applicationContext)
        CallBlockNotificationSettings.init(applicationContext)
        CallBlockNotifier.ensureChannel(applicationContext)
        // Upgrade bootstrap: build the durable screening snapshot in the background while the app
        // is open, so the next cold incoming call does not need to open/wait for Room.
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { CallBlockRepository(appContext).warmScreeningRuleSnapshot() }
                .onFailure { error -> Log.e("CallBlockWarmup", "Unable to warm screening rules", error) }
        }
        // Nạp PHẠM VI SIM toàn app đã lưu TRƯỚC setContent → nhật ký/thống kê khung hình đầu đã đúng SIM đang xem.
        SimScope.init(applicationContext)
        // Mở nguồn phản ứng NHÓM PHÂN LOẠI (seed 2 nhóm mặc định + collector Room) → badge/chip cập nhật toàn app.
        CategoryCatalog.start(applicationContext)
        // Thanh trạng thái + điều hướng: trong suốt; icon TỐI ở chế độ sáng, SÁNG ở chế độ tối.
        val dark = ThemeSettings.dark
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle(dark),
            navigationBarStyle = systemBarStyle(dark)
        )
        applyBars(dark)
        setContent {
            CallHSTheme {
                // Đồng bộ icon thanh hệ thống theo chế độ ĐÃ GIẢI (kể cả khi đổi trong app — không qua
                // onConfigurationChanged): tối → icon sáng, sáng → icon tối.
                val isDark = ThemeSettings.dark
                LaunchedEffect(isDark) { applyBars(isDark) }
                // Nền ở gốc theo chế độ: đảm bảo vùng SAU thanh trạng thái/điều hướng luôn đúng màu trên mọi
                // phiên bản 10→16 (thay cho window.navigationBarColor đã bị vô hiệu hoá ở API 35+).
                Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
                    // Cổng quyền BẮT BUỘC: xin lần lượt 3 quyền (nhật ký cuộc gọi → thông tin SIM → danh bạ)
                    // theo thứ tự rõ ràng trước khi vào ứng dụng.
                    PermissionGate { AppNav() }
                    // Host toast DUY NHẤT điều phối queue toàn app; host trong dialog/sheet chỉ nhận phần hiển thị
                    // để toast luôn nổi trên đúng cửa sổ đang ở trên cùng.
                    AppToastHost(modifier = Modifier.fillMaxSize(), isRootHost = true)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Đổi ngôn ngữ MÁY lúc đang mở app → giải lại (chỉ khi đang ở chế độ "Theo hệ thống").
        LanguageSettings.onConfigChanged()
        // Đổi Dark/Light của MÁY lúc đang mở app (uiMode nằm trong configChanges nên KHÔNG dựng lại Activity) →
        // cập nhật state (đọc cấu hình THẬT của thiết bị) + đồng bộ icon thanh hệ thống. Thay đổi state sẽ khiến
        // CallHSTheme recompose (tô lại) + LaunchedEffect(dark) chạy applyBars.
        refreshSystemDark()
    }

    override fun onResume() {
        super.onResume()
        // An toàn dự phòng: nếu người dùng đổi Sáng/Tối của MÁY lúc app ở NỀN (hoặc khung onConfigurationChanged
        // không được gọi vì cấu hình Activity bị ghim), thì lúc quay lại app vẫn đồng bộ đúng chế độ hệ thống.
        refreshSystemDark()
    }

    /** Đọc chế độ tối THẬT của thiết bị và đẩy vào [ThemeSettings] (có guard idempotent) + cập nhật thanh hệ thống. */
    private fun refreshSystemDark() {
        ThemeSettings.setSystemDark(ThemeSettings.isSystemNight())
        applyBars(ThemeSettings.dark)
    }

    /** Kiểu thanh hệ thống (trong suốt) theo chế độ: tối → nền tối/icon sáng, sáng → nền sáng/icon tối. */
    private fun systemBarStyle(dark: Boolean): SystemBarStyle =
        if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)

    /**
     * Đồng bộ icon thanh trạng thái + điều hướng theo chế độ. Ở chế độ SÁNG dùng icon TỐI (nền sáng), ở chế độ
     * TỐI dùng icon SÁNG (nền tối). Nền "màu" của thanh đến từ nội dung vẽ tràn phía sau (Box nền ở setContent).
     *
     * Lưu ý API 35+: window.navigationBarColor và isNavigationBarContrastEnforced đã thành no-op, nên KHÔNG đặt
     * màu thanh ở đây; riêng isNavigationBarContrastEnforced=false vẫn hữu ích ở API 29-34 (tắt lớp phủ tự động).
     */
    private fun applyBars(dark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
