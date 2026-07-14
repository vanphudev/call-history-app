package com.antimobile.callhs.util

import android.content.Context

/**
 * Đọc `versionCode` / `versionName` của chính ứng dụng lúc CHẠY.
 *
 * KHÔNG dùng `BuildConfig` vì `buildFeatures.buildConfig` đang TẮT trong `app/build.gradle.kts` — lớp
 * `BuildConfig` không được sinh ra. `longVersionCode` cần API 28, `minSdk = 29` nên luôn an toàn.
 */
object AppVersion {

    /** `versionCode` khai trong `build.gradle.kts`; `0` nếu không đọc được (thực tế không xảy ra). */
    @Suppress("DEPRECATION")
    fun code(context: Context): Int = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
    }.getOrDefault(0)

    /** `versionName` khai trong `build.gradle.kts` (vd "26.1.1"); chuỗi rỗng nếu không đọc được. */
    @Suppress("DEPRECATION")
    fun name(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
}
