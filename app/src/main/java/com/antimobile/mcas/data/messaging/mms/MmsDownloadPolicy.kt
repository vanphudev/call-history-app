package com.antimobile.mcas.data.messaging.mms

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/** Chính sách bảo thủ: tự tải khi không roaming; roaming luôn cần thao tác rõ ràng của người dùng. */
object MmsDownloadPolicy {
    private const val PREFS = "mms_download_policy_v1"
    private const val KEY_AUTO = "auto_download"
    private const val KEY_ROAMING = "auto_download_roaming"

    fun isAutoDownloadEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO, true)

    fun isAutoDownloadWhileRoamingEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ROAMING, false)

    fun setAutoDownloadEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO, enabled).apply()
    }

    fun setAutoDownloadWhileRoamingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ROAMING, enabled).apply()
    }

    @SuppressLint("MissingPermission") // ROLE_SMS grants phone state; runCatching keeps revoked-permission fallback safe.
    fun shouldAutoDownload(context: Context, subscriptionId: Int?): Boolean {
        if (!isAutoDownloadEnabled(context)) return false
        val subId = subscriptionId?.takeIf { SubscriptionManager.isValidSubscriptionId(it) } ?: return false
        val telephony = context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId)
        val dataEnabled = runCatching { telephony.isDataEnabled }.getOrDefault(true)
        if (!dataEnabled) return false
        val roaming = runCatching { telephony.isNetworkRoaming }.getOrDefault(false)
        return !roaming || isAutoDownloadWhileRoamingEnabled(context)
    }
}
