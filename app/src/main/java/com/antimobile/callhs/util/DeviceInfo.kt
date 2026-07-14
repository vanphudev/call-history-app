package com.antimobile.callhs.util

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Khu vực (region) của THIẾT BỊ — suy theo thứ tự: quốc gia của SIM → của mạng → locale hệ thống,
 * hiển thị tên tiếng Việt (vd "Việt Nam"). Mặc định "Việt Nam" nếu không xác định được.
 * Khởi tạo 1 lần lúc mở app ([init]); các nơi khác đọc [regionName].
 */
object DeviceInfo {

    @Volatile
    var regionName: String = "Việt Nam"
        private set

    fun init(context: Context) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val iso = sequenceOf(
            tm?.simCountryIso,
            tm?.networkCountryIso,
            Locale.getDefault().country
        ).firstOrNull { !it.isNullOrBlank() }?.uppercase()

        regionName = iso
            ?.let { runCatching { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.forLanguageTag("vi-VN")) }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
            ?: "Việt Nam"
    }
}
