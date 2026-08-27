package com.antimobile.callhs.data.outgoing

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.antimobile.callhs.MainActivity
import com.antimobile.callhs.R
import com.antimobile.callhs.i18n.OutgoingCallStrings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.util.formatPhone

/** Heads-up riêng của tính năng cuộc gọi đi; không dùng channel/cài đặt thông báo chặn cuộc gọi. */
object OutgoingCallNotifier {
    internal const val CHANNEL_ID = "outgoing_call_alerts_v1"
    private const val LOG_TAG = "OutgoingCallNotifier"

    enum class Readiness {
        READY,
        RUNTIME_PERMISSION_REQUIRED,
        APP_NOTIFICATIONS_DISABLED,
        CHANNEL_DISABLED,
        CHANNEL_NOT_URGENT,
    }

    fun ensureChannel(context: Context) {
        val strings = appStrings().outgoingCall
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings.notificationChannelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = strings.notificationChannelDescription
            enableVibration(true)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun readiness(context: Context): Readiness {
        ensureChannel(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Readiness.RUNTIME_PERMISSION_REQUIRED
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return Readiness.APP_NOTIFICATIONS_DISABLED
        }
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL_ID)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            return Readiness.CHANNEL_DISABLED
        }
        if (channel == null || channel.importance < NotificationManager.IMPORTANCE_HIGH) {
            return Readiness.CHANNEL_NOT_URGENT
        }
        return Readiness.READY
    }

    fun openSettings(context: Context) {
        ensureChannel(context)
        val channelIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(channelIntent) }
            .recoverCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            .onFailure { Log.w(LOG_TAG, "Unable to open notification settings", it) }
    }

    fun notify(context: Context, event: OutgoingCallAlertEvent): Boolean {
        if (!event.reasons.any()) return false
        ensureChannel(context)
        if (readiness(context) in setOf(
                Readiness.RUNTIME_PERMISSION_REQUIRED,
                Readiness.APP_NOTIFICATIONS_DISABLED,
                Readiness.CHANNEL_DISABLED,
            )
        ) return false

        val strings = appStrings().outgoingCall
        val title = alertTitle(event, strings)
        val body = alertBody(event, strings)
        val id = notificationId(event)
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_outgoing_call)
            .setColor(Color.rgb(52, 168, 83))
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setWhen(event.createdAtMillis)
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .build()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        return runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        }.onFailure { Log.e(LOG_TAG, "Unable to post outgoing-call alert", it) }
            .getOrDefault(false)
    }

    internal fun alertTitle(event: OutgoingCallAlertEvent, strings: OutgoingCallStrings): String = when {
        OutgoingCallAlertReason.BLOCKLIST in event.reasons -> strings.alertBlocklistTitle
        OutgoingCallAlertReason.OFF_NETWORK in event.reasons -> strings.alertOffNetworkTitle
        else -> strings.alertAllowlistTitle
    }

    internal fun alertBody(event: OutgoingCallAlertEvent, strings: OutgoingCallStrings): String =
        buildList {
            add(formatPhone(event.number))
            event.reasons.forEach { reason ->
                when (reason) {
                    OutgoingCallAlertReason.BLOCKLIST -> add(strings.reasonBlocklist)
                    OutgoingCallAlertReason.ALLOWLIST -> add(strings.reasonAllowlist)
                    OutgoingCallAlertReason.OFF_NETWORK -> {
                        val sim = event.simCarrier
                        val target = event.targetCarrier
                        if (sim != null && target != null) add(strings.reasonOffNetwork(sim, target))
                    }
                }
            }
        }.joinToString("\n")

    private fun notificationId(event: OutgoingCallAlertEvent): Int {
        val identity = event.number.hashCode().toLong() shl Int.SIZE_BITS
        val value = event.createdAtMillis xor identity
        val folded = (value xor (value ushr Int.SIZE_BITS)).toInt() and Int.MAX_VALUE
        return folded.takeIf { it != 0 } ?: 1
    }
}
