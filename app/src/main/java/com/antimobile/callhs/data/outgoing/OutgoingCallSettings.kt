package com.antimobile.callhs.data.outgoing

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Kho cài đặt RIÊNG của tính năng cảnh báo cuộc gọi đi.
 *
 * Service luôn dùng [read] để hỗ trợ tiến trình khởi động lạnh; các state bên dưới chỉ là bản sao cho
 * Compose và không phải điều kiện để luồng nền hoạt động.
 */
object OutgoingCallSettings {
    private const val PREFS = "outgoing_call_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_NOTIFY_OFF_NETWORK = "notify_off_network"
    private const val KEY_NOTIFY_BLOCKLIST = "notify_blocklist"
    private const val KEY_NOTIFY_ALLOWLIST = "notify_allowlist"
    private const val KEY_PRESENTATION = "presentation"

    var enabled by mutableStateOf(false)
        private set
    var notifyOffNetwork by mutableStateOf(true)
        private set
    var notifyBlocklist by mutableStateOf(true)
        private set
    var notifyAllowlist by mutableStateOf(true)
        private set
    var presentation by mutableStateOf(OutgoingCallPresentation.HEADS_UP)
        private set

    fun init(context: Context) {
        publish(read(context))
    }

    fun read(context: Context): OutgoingCallConfig {
        val preferences = prefs(context)
        return OutgoingCallConfig(
            enabled = preferences.safeBoolean(KEY_ENABLED, false),
            notifyOffNetwork = preferences.safeBoolean(KEY_NOTIFY_OFF_NETWORK, true),
            notifyBlocklist = preferences.safeBoolean(KEY_NOTIFY_BLOCKLIST, true),
            notifyAllowlist = preferences.safeBoolean(KEY_NOTIFY_ALLOWLIST, true),
            presentation = OutgoingCallPresentation.fromStorage(
                preferences.safeString(KEY_PRESENTATION),
            ),
        )
    }

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, value) }
        enabled = value
    }

    fun setNotifyOffNetwork(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_NOTIFY_OFF_NETWORK, value) }
        notifyOffNetwork = value
    }

    fun setNotifyBlocklist(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_NOTIFY_BLOCKLIST, value) }
        notifyBlocklist = value
    }

    fun setNotifyAllowlist(context: Context, value: Boolean) {
        prefs(context).edit { putBoolean(KEY_NOTIFY_ALLOWLIST, value) }
        notifyAllowlist = value
    }

    fun setPresentation(context: Context, value: OutgoingCallPresentation) {
        prefs(context).edit { putString(KEY_PRESENTATION, value.storageKey) }
        presentation = value
    }

    /** Ghi một snapshot hoàn chỉnh bằng đúng một transaction SharedPreferences khi khôi phục backup. */
    fun replace(context: Context, config: OutgoingCallConfig) {
        prefs(context).edit {
            putBoolean(KEY_ENABLED, config.enabled)
            putBoolean(KEY_NOTIFY_OFF_NETWORK, config.notifyOffNetwork)
            putBoolean(KEY_NOTIFY_BLOCKLIST, config.notifyBlocklist)
            putBoolean(KEY_NOTIFY_ALLOWLIST, config.notifyAllowlist)
            putString(KEY_PRESENTATION, config.presentation.storageKey)
        }
        publish(config)
    }

    private fun publish(config: OutgoingCallConfig) {
        enabled = config.enabled
        notifyOffNetwork = config.notifyOffNetwork
        notifyBlocklist = config.notifyBlocklist
        notifyAllowlist = config.notifyAllowlist
        presentation = config.presentation
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun SharedPreferences.safeBoolean(key: String, default: Boolean): Boolean =
        runCatching { getBoolean(key, default) }.getOrDefault(default)

    private fun SharedPreferences.safeString(key: String): String? =
        runCatching { getString(key, null) }.getOrNull()
}
