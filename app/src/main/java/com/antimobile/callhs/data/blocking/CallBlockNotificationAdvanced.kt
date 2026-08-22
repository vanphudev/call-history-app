package com.antimobile.callhs.data.blocking

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Four packaged sounds. Storage keys are stable across resource-ID changes. */
enum class BlockNotificationSoundPreset(val storageKey: String) {
    PULSE("pulse"),
    RIPPLE("ripple"),
    BAMBOO("bamboo"),
    CRYSTAL("crystal");

    companion object {
        fun fromStorage(value: String?): BlockNotificationSoundPreset? =
            entries.firstOrNull { it.storageKey == value }
    }
}

/** A packaged preset or a persistable SAF content URI chosen by the user. */
data class BlockNotificationSound(
    val storageKey: String = BlockNotificationSoundPreset.PULSE.storageKey,
    val displayName: String? = null,
) {
    val preset: BlockNotificationSoundPreset?
        get() = BlockNotificationSoundPreset.fromStorage(storageKey)

    val customUri: Uri?
        get() = storageKey
            .takeIf { it.startsWith(CUSTOM_PREFIX) }
            ?.removePrefix(CUSTOM_PREFIX)
            ?.let(Uri::parse)
            ?.takeIf { it.scheme == "content" }

    fun normalized(): BlockNotificationSound =
        if (preset != null || customUri != null) this else BlockNotificationSound()

    companion object {
        private const val CUSTOM_PREFIX = "custom:"

        fun preset(value: BlockNotificationSoundPreset) =
            BlockNotificationSound(storageKey = value.storageKey)

        fun custom(uri: Uri, displayName: String) = BlockNotificationSound(
            storageKey = "$CUSTOM_PREFIX$uri",
            displayName = displayName.trim().take(120),
        )
    }
}

data class BlockNotificationAlert(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val sound: BlockNotificationSound = BlockNotificationSound(),
    val presentation: BlockNotificationPresentation = BlockNotificationPresentation.HEADS_UP,
) {
    fun normalized(): BlockNotificationAlert = copy(sound = sound.normalized())
}

/** How prominently a blocked-call notification is presented by Android. */
enum class BlockNotificationPresentation(val storageKey: String) {
    STATUS_BAR("status_bar"),
    HEADS_UP("heads_up");

    companion object {
        fun fromStorage(value: String?): BlockNotificationPresentation =
            entries.firstOrNull { it.storageKey == value } ?: HEADS_UP
    }
}

/** Fixed, gap-free day partition. Night intentionally crosses midnight. */
enum class BlockNotificationPeriod(
    val storageKey: String,
    val startMinute: Int,
    val endMinute: Int,
) {
    MORNING("morning", 6 * 60, 12 * 60),
    AFTERNOON("afternoon", 12 * 60, 18 * 60),
    EVENING("evening", 18 * 60, 22 * 60),
    NIGHT("night", 22 * 60, 6 * 60);

    fun contains(minuteOfDay: Int): Boolean {
        val minute = Math.floorMod(minuteOfDay, MINUTES_PER_DAY)
        return if (startMinute < endMinute) {
            minute in startMinute until endMinute
        } else {
            minute >= startMinute || minute < endMinute
        }
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
    }
}

data class BlockNotificationPeriodSettings(
    val period: BlockNotificationPeriod,
    val enabled: Boolean = false,
    val alert: BlockNotificationAlert = BlockNotificationAlert(),
)

data class BlockNotificationAdvancedConfig(
    val defaultAlert: BlockNotificationAlert = BlockNotificationAlert(),
    val scheduleEnabled: Boolean = false,
    val periods: List<BlockNotificationPeriodSettings> = defaultPeriods(),
) {
    fun normalized(): BlockNotificationAdvancedConfig {
        val configured = periods.associateBy { it.period }
        return copy(
            defaultAlert = defaultAlert.normalized(),
            periods = BlockNotificationPeriod.entries.map { period ->
                configured[period]
                    ?.copy(alert = configured.getValue(period).alert.normalized())
                    ?: BlockNotificationPeriodSettings(period)
            },
        )
    }

    fun period(value: BlockNotificationPeriod): BlockNotificationPeriodSettings =
        periods.firstOrNull { it.period == value } ?: BlockNotificationPeriodSettings(value)

    /** Default and scheduled behavior are deliberately mutually exclusive. */
    fun alertAt(minuteOfDay: Int): BlockNotificationAlert? {
        if (!scheduleEnabled) return defaultAlert
        val activePeriod = BlockNotificationPeriod.entries.first { it.contains(minuteOfDay) }
        return period(activePeriod).takeIf { it.enabled }?.alert
    }

    companion object {
        fun defaultPeriods(): List<BlockNotificationPeriodSettings> =
            BlockNotificationPeriod.entries.map(::BlockNotificationPeriodSettings)
    }
}

sealed interface BlockNotificationSoundImportResult {
    data class Success(val sound: BlockNotificationSound) : BlockNotificationSoundImportResult
    data class Error(val reason: Reason) : BlockNotificationSoundImportResult

    enum class Reason {
        NOT_AUDIO,
        TOO_LARGE,
        TOO_LONG,
        EMPTY_OR_INVALID,
        CANNOT_READ,
        CANNOT_KEEP_PERMISSION,
    }
}

/** Durable advanced settings, kept separate from the notification cadence preference. */
object CallBlockNotificationSettings {
    private const val PREFS = "call_block_notification_settings"
    private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val MAX_FILE_BYTES = 10L * 1024L * 1024L
    private const val MAX_DURATION_MILLIS = 30_000L
    private val AUDIO_EXTENSIONS = setOf("aac", "flac", "m4a", "mp3", "oga", "ogg", "wav")

    var config by mutableStateOf(BlockNotificationAdvancedConfig())
        private set

    fun init(context: Context) {
        config = read(context)
    }

    /** Authoritative read for the call-screening process. */
    fun read(context: Context): BlockNotificationAdvancedConfig {
        val preferences = prefs(context)
        return BlockNotificationAdvancedConfig(
            defaultAlert = readAlert(preferences, "default"),
            scheduleEnabled = preferences.getBoolean(KEY_SCHEDULE_ENABLED, false),
            periods = BlockNotificationPeriod.entries.map { period ->
                val prefix = period.storageKey
                BlockNotificationPeriodSettings(
                    period = period,
                    enabled = preferences.getBoolean("${prefix}_enabled", false),
                    alert = readAlert(preferences, prefix),
                )
            },
        ).normalized()
    }

    fun alertAt(context: Context, minuteOfDay: Int): BlockNotificationAlert? =
        read(context).alertAt(minuteOfDay)

    fun setScheduleEnabled(context: Context, enabled: Boolean): Boolean =
        save(context, config.copy(scheduleEnabled = enabled))

    fun setDefaultAlert(context: Context, alert: BlockNotificationAlert): Boolean =
        save(context, config.copy(defaultAlert = alert))

    fun setPeriod(context: Context, value: BlockNotificationPeriodSettings): Boolean =
        save(
            context,
            config.copy(periods = config.periods.map { if (it.period == value.period) value else it }),
        )

    fun validateAndPersistCustomSound(
        context: Context,
        uri: Uri,
    ): BlockNotificationSoundImportResult {
        if (uri.scheme != "content") {
            return BlockNotificationSoundImportResult.Error(
                BlockNotificationSoundImportResult.Reason.CANNOT_READ
            )
        }
        val resolver = context.contentResolver
        val permissionKept = runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        }.getOrDefault(false)
        if (!permissionKept) {
            return BlockNotificationSoundImportResult.Error(
                BlockNotificationSoundImportResult.Reason.CANNOT_KEEP_PERMISSION
            )
        }

        fun reject(reason: BlockNotificationSoundImportResult.Reason): BlockNotificationSoundImportResult {
            runCatching { resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            return BlockNotificationSoundImportResult.Error(reason)
        }

        val metadata = runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                name to size
            }
        }.getOrNull()
        val displayName = metadata?.first?.trim().orEmpty().ifBlank { "Âm thanh tùy chỉnh" }
        val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = runCatching { resolver.getType(uri)?.lowercase(Locale.ROOT) }.getOrNull()
        if (mime?.startsWith("audio/") != true && extension !in AUDIO_EXTENSIONS) {
            return reject(BlockNotificationSoundImportResult.Reason.NOT_AUDIO)
        }
        if ((metadata?.second ?: 0L) > MAX_FILE_BYTES) {
            return reject(BlockNotificationSoundImportResult.Reason.TOO_LARGE)
        }

        val actualSize = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (total <= MAX_FILE_BYTES) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                }
                total
            }
        }.getOrNull() ?: return reject(BlockNotificationSoundImportResult.Reason.CANNOT_READ)
        if (actualSize == 0L) return reject(BlockNotificationSoundImportResult.Reason.EMPTY_OR_INVALID)
        if (actualSize > MAX_FILE_BYTES) return reject(BlockNotificationSoundImportResult.Reason.TOO_LARGE)

        val duration = runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }
        }.getOrNull()
        if (duration == null || duration <= 0L) {
            return reject(BlockNotificationSoundImportResult.Reason.EMPTY_OR_INVALID)
        }
        if (duration > MAX_DURATION_MILLIS) {
            return reject(BlockNotificationSoundImportResult.Reason.TOO_LONG)
        }
        return BlockNotificationSoundImportResult.Success(
            BlockNotificationSound.custom(uri, displayName)
        )
    }

    @SuppressLint("UseKtx") // The KTX helper discards commit(), but UI state needs its result.
    private fun save(context: Context, updated: BlockNotificationAdvancedConfig): Boolean {
        val normalized = updated.normalized()
        val editor = prefs(context).edit()
            .putBoolean(KEY_SCHEDULE_ENABLED, normalized.scheduleEnabled)
        writeAlert(editor, "default", normalized.defaultAlert)
        normalized.periods.forEach { period ->
            editor.putBoolean("${period.period.storageKey}_enabled", period.enabled)
            writeAlert(editor, period.period.storageKey, period.alert)
        }
        return editor.commit().also { saved -> if (saved) config = normalized }
    }

    private fun readAlert(
        preferences: android.content.SharedPreferences,
        prefix: String,
    ) = BlockNotificationAlert(
        soundEnabled = preferences.getBoolean("${prefix}_sound_enabled", true),
        vibrationEnabled = preferences.getBoolean("${prefix}_vibration_enabled", true),
        sound = BlockNotificationSound(
            storageKey = preferences.getString(
                "${prefix}_sound",
                BlockNotificationSoundPreset.PULSE.storageKey,
            ) ?: BlockNotificationSoundPreset.PULSE.storageKey,
            displayName = preferences.getString("${prefix}_sound_name", null),
        ),
        presentation = BlockNotificationPresentation.fromStorage(
            preferences.getString("${prefix}_presentation", null)
        ),
    ).normalized()

    private fun writeAlert(
        editor: android.content.SharedPreferences.Editor,
        prefix: String,
        alert: BlockNotificationAlert,
    ) {
        editor
            .putBoolean("${prefix}_sound_enabled", alert.soundEnabled)
            .putBoolean("${prefix}_vibration_enabled", alert.vibrationEnabled)
            .putString("${prefix}_sound", alert.sound.storageKey)
            .putString("${prefix}_sound_name", alert.sound.displayName)
            .putString("${prefix}_presentation", alert.presentation.storageKey)
    }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
