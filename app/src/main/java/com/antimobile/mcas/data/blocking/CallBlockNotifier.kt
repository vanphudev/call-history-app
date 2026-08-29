package com.antimobile.mcas.data.blocking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.antimobile.mcas.MainActivity
import com.antimobile.mcas.R
import com.antimobile.mcas.i18n.CallBlockStrings
import com.antimobile.mcas.i18n.Lang
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.util.formatPhone
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalTime

/** Notification do MCAS tự gửi sau khi đã chặn cuộc gọi qua CallScreeningService. */
object CallBlockNotifier {
    /** Android freezes channel sound/vibration, so advanced profiles get immutable channel IDs. */
    internal const val CHANNEL_ID = "blocked_calls_urgent_sound_v4"
    private val LEGACY_CHANNEL_IDS = arrayOf(
        "blocked_calls",
        "blocked_calls_urgent_v2",
        "blocked_calls_urgent_sound_v3",
    )
    private const val LOG_TAG = "CallBlockNotifier"
    private val VIBRATION_PATTERN = longArrayOf(0L, 180L, 90L, 260L)

    enum class Readiness {
        READY,
        RUNTIME_PERMISSION_REQUIRED,
        APP_NOTIFICATIONS_DISABLED,
        CHANNEL_DISABLED,
        CHANNEL_NOT_URGENT,
        CHANNEL_MUTED,
    }

    enum class DeliveryResult {
        POSTED,
        SKIPPED_NOT_NEW,
        SKIPPED_DISABLED,
        BLOCKED_BY_SYSTEM,
        FAILED,
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val config = CallBlockNotificationSettings.read(context)
        val alert = config.alertAt(currentMinuteOfDay()) ?: config.defaultAlert
        ensureChannel(context, alert)
    }

    private fun ensureChannel(context: Context, alert: BlockNotificationAlert) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val s = appStrings().blocker
        val manager = context.getSystemService(NotificationManager::class.java)
        val soundUri = alert.sound.takeIf { alert.soundEnabled }?.let { notificationSoundUri(context, it) }
        val channelId = channelId(alert)
        val channel = NotificationChannel(
            channelId,
            "${s.notificationChannelName} · ${channelProfileLabel(alert)}",
            channelImportance(alert.presentation),
        ).apply {
            description = s.notificationChannelDescription
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(alert.vibrationEnabled)
            vibrationPattern = if (alert.vibrationEnabled) VIBRATION_PATTERN else null
            enableLights(true)
            lightColor = Color.rgb(30, 142, 62)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        // Keep Android's settings page unambiguous: these channels are no longer used and cannot
        // be repaired in place because their auditory behavior is immutable.
        LEGACY_CHANNEL_IDS.forEach { legacyId ->
            if (legacyId != channelId && manager.getNotificationChannel(legacyId) != null) {
                manager.deleteNotificationChannel(legacyId)
            }
        }
    }

    /**
     * Reports both posting permission and the channel properties required for sound + heads-up.
     * Apps cannot raise a channel again after the user/system has lowered it, so the UI must send
     * the user to Android's channel settings instead of pretending the alert is ready.
     */
    fun readiness(context: Context): Readiness {
        val config = CallBlockNotificationSettings.read(context)
        val alert = config.alertAt(currentMinuteOfDay()) ?: config.defaultAlert
        ensureChannel(context, alert)
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(channelId(alert))
        } else {
            null
        }
        val expectedSound = alert.sound.takeIf { alert.soundEnabled }?.let { notificationSoundUri(context, it) }
        return evaluateReadiness(
            permissionGranted = permissionGranted,
            appNotificationsEnabled = notificationsEnabled,
            channelImportance = channel?.importance,
            channelHasSound = channel?.sound != null,
            channelUsesAppSound = channel == null || !alert.soundEnabled || channel.sound == expectedSound,
            soundExpected = alert.soundEnabled,
            vibrationExpected = alert.vibrationEnabled,
            channelVibrates = channel?.shouldVibrate() == true,
            minimumChannelImportance = channelImportance(alert.presentation),
        )
    }

    fun openNotificationSettings(context: Context) {
        val config = CallBlockNotificationSettings.read(context)
        val alert = config.alertAt(currentMinuteOfDay()) ?: config.defaultAlert
        ensureChannel(context, alert)
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId(alert))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .recoverCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            .onFailure { Log.w(LOG_TAG, "Unable to open Android notification settings", it) }
    }

    fun notifyBlocked(
        context: Context,
        result: BlockRecordResult,
        notificationEventId: Long = result.historyId,
    ): DeliveryResult {
        if (!result.isNew) return DeliveryResult.SKIPPED_NOT_NEW
        val mode = CallBlockSettings.notificationMode(context)
        if (!CallBlockSettings.shouldNotify(mode)) {
            return DeliveryResult.SKIPPED_DISABLED
        }

        val alert = CallBlockNotificationSettings.alertAt(context, currentMinuteOfDay())
            ?: return DeliveryResult.SKIPPED_DISABLED
        val activeChannelId = channelId(alert)
        val activeSoundUri = alert.sound.takeIf { alert.soundEnabled }
            ?.let { notificationSoundUri(context, it) }

        ensureChannel(context, alert)
        when (readiness(context)) {
            Readiness.RUNTIME_PERMISSION_REQUIRED,
            Readiness.APP_NOTIFICATIONS_DISABLED,
            Readiness.CHANNEL_DISABLED,
            -> {
                Log.w(LOG_TAG, "Blocked-call notification suppressed by Android settings")
                return DeliveryResult.BLOCKED_BY_SYSTEM
            }

            // The notification still belongs in the shade if the user lowered/muted the channel.
            // The settings UI separately explains that heads-up or sound needs restoring.
            Readiness.CHANNEL_NOT_URGENT,
            Readiness.CHANNEL_MUTED,
            Readiness.READY,
            -> Unit
        }

        val s = appStrings().blocker
        val reason = notificationReason(result, s)
        val body = s.notificationBody(
            result.totalForNumber,
            reason,
        )
        val requestCode = notificationId(notificationEventId)
        val openApp = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, activeChannelId)
            .setSmallIcon(R.drawable.ic_notification_blocked_call)
            .setColor(Color.rgb(30, 142, 62))
            .setContentTitle(s.notificationTitle(formatPhone(result.rawNumber)))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(
                if (alert.presentation == BlockNotificationPresentation.HEADS_UP) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            // Ignored in favor of the channel on Android 8+, but retained as an explicit fallback
            // for vendor implementations that still inspect notification-level sound metadata.
            .setSound(activeSoundUri)
            .setVibrate(if (alert.vibrationEnabled) VIBRATION_PATTERN else null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            // The service posts immediately after Telecom responds, then may update the same event
            // with the durable Room count. Only the first post must make sound/vibrate.
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(openApp)
            .build()

        // Readiness was checked above, but keep the permission guard adjacent to notify(): the
        // permission can be revoked while the service is evaluating a call, and Android lint can
        // also verify this path without relying on the higher-level readiness abstraction.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return DeliveryResult.BLOCKED_BY_SYSTEM

        return runCatching {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            DeliveryResult.POSTED
        }.getOrElse { error ->
            Log.e(LOG_TAG, "Failed to post blocked-call notification", error)
            DeliveryResult.FAILED
        }
    }

    /** Keeps the persisted guard reason distinct from user-authored rule summaries. */
    @VisibleForTesting
    internal fun notificationReason(result: BlockRecordResult, s: CallBlockStrings): String {
        val specialized = when (result.historyReasonType) {
            REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE ->
                RepeatUnknownCallerGuardReasonCodec.decode(result.historyReasonValue)?.let { guard ->
                s.repeatCallerGuardReason(
                    attempt = guard.attempt,
                    threshold = guard.threshold,
                    minutes = guard.windowMinutes,
                )
            }
            CallBlockRuleType.SPAM_RISK.storageKey ->
                SpamRiskReasonCodec.decode(result.historyReasonValue)?.let { reason ->
                    when (reason.kind) {
                        SpamRiskReasonKind.PREFIX -> s.spamRiskReasonPrefix(reason.prefix)
                        SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX ->
                            s.spamRiskReasonUnknownMobilePrefix(reason.prefix)
                        SpamRiskReasonKind.VERIFICATION_FAILED -> s.spamRiskReasonVerificationFailed
                    }
                }
            else -> null
        }
        return specialized ?: s.ruleSummary(result.ruleType, result.ruleValue)
    }

    internal fun notificationSoundUri(context: Context): Uri {
        return notificationSoundUri(
            context,
            BlockNotificationSound.preset(BlockNotificationSoundPreset.PULSE),
        )
    }

    internal fun notificationSoundUri(context: Context, sound: BlockNotificationSound): Uri {
        sound.customUri?.let { customUri ->
            val readable = runCatching {
                context.contentResolver.openAssetFileDescriptor(customUri, "r")?.use { true } == true
            }.getOrDefault(false)
            if (readable) return customUri
        }
        // Reference R.raw directly so release resource shrinking cannot remove the sound. The URI
        // still stores its stable type/name rather than an aapt numeric ID, which may change after
        // an app upgrade while Android keeps the notification channel.
        val resourceId = when (sound.preset ?: BlockNotificationSoundPreset.PULSE) {
            BlockNotificationSoundPreset.PULSE -> R.raw.mcas_pulse
            BlockNotificationSoundPreset.RIPPLE -> R.raw.mcas_ripple
            BlockNotificationSoundPreset.BAMBOO -> R.raw.mcas_bamboo
            BlockNotificationSoundPreset.CRYSTAL -> R.raw.mcas_crystal
        }
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(context.resources.getResourceTypeName(resourceId))
            .appendPath(context.resources.getResourceEntryName(resourceId))
            .build()
    }

    @VisibleForTesting
    internal fun evaluateReadiness(
        permissionGranted: Boolean,
        appNotificationsEnabled: Boolean,
        channelImportance: Int?,
        channelHasSound: Boolean,
        channelUsesAppSound: Boolean = true,
        soundExpected: Boolean = true,
        vibrationExpected: Boolean = false,
        channelVibrates: Boolean = true,
        minimumChannelImportance: Int = NotificationManager.IMPORTANCE_HIGH,
    ): Readiness = when {
        !permissionGranted -> Readiness.RUNTIME_PERMISSION_REQUIRED
        !appNotificationsEnabled -> Readiness.APP_NOTIFICATIONS_DISABLED
        channelImportance == NotificationManager.IMPORTANCE_NONE -> Readiness.CHANNEL_DISABLED
        channelImportance == null || channelImportance < minimumChannelImportance ->
            Readiness.CHANNEL_NOT_URGENT
        soundExpected && (!channelHasSound || !channelUsesAppSound) -> Readiness.CHANNEL_MUTED
        vibrationExpected && !channelVibrates -> Readiness.CHANNEL_MUTED
        else -> Readiness.READY
    }

    private fun channelId(alert: BlockNotificationAlert): String {
        if (
            alert.soundEnabled &&
            alert.vibrationEnabled &&
            alert.presentation == BlockNotificationPresentation.HEADS_UP &&
            alert.sound == BlockNotificationSound.preset(BlockNotificationSoundPreset.PULSE)
        ) return CHANNEL_ID
        val identity = listOf(
            alert.soundEnabled.toString(),
            alert.vibrationEnabled.toString(),
            alert.sound.storageKey,
            alert.presentation.storageKey,
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(StandardCharsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
        return "blocked_calls_profile_v4_$digest"
    }

    private fun channelProfileLabel(alert: BlockNotificationAlert): String = buildString {
        val vietnamese = LanguageSettings.lang == Lang.VI
        append(when {
            alert.soundEnabled && alert.vibrationEnabled -> if (vietnamese) "âm thanh + rung" else "sound + vibration"
            alert.soundEnabled -> if (vietnamese) "âm thanh" else "sound"
            alert.vibrationEnabled -> if (vietnamese) "rung" else "vibration"
            else -> if (vietnamese) "im lặng" else "silent"
        })
        append(
            if (alert.presentation == BlockNotificationPresentation.HEADS_UP) {
                if (vietnamese) " · nổi" else " · heads-up"
            } else {
                if (vietnamese) " · thanh trạng thái" else " · status bar"
            }
        )
    }

    private fun channelImportance(presentation: BlockNotificationPresentation): Int =
        if (presentation == BlockNotificationPresentation.HEADS_UP) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }

    private fun currentMinuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    @VisibleForTesting
    internal fun notificationId(historyId: Long): Int {
        val folded = (historyId xor (historyId ushr Int.SIZE_BITS)).toInt() and Int.MAX_VALUE
        return folded.takeIf { it != 0 } ?: 1
    }
}
