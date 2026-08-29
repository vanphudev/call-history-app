package com.antimobile.mcas.data.messaging.mms

import android.content.Context
import android.telephony.SmsManager
import com.antimobile.mcas.data.messaging.transport.SmsManagerFactory

data class MmsCarrierLimits(
    val enabled: Boolean,
    val maxMessageBytes: Int,
    val maxImageWidth: Int,
    val maxImageHeight: Int,
    val maxSubjectLength: Int,
) {
    companion object {
        fun load(context: Context, subscriptionId: Int): MmsCarrierLimits {
            val config = runCatching {
                SmsManagerFactory.forSubscription(context, subscriptionId).carrierConfigValues
            }.getOrNull()
            return MmsCarrierLimits(
                enabled = config?.getBoolean(SmsManager.MMS_CONFIG_MMS_ENABLED, true) ?: true,
                maxMessageBytes = (config?.getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, DEFAULT_MAX_BYTES)
                    ?: DEFAULT_MAX_BYTES).coerceIn(80 * 1024, 3 * 1024 * 1024),
                maxImageWidth = (config?.getInt(SmsManager.MMS_CONFIG_MAX_IMAGE_WIDTH, DEFAULT_MAX_DIMENSION)
                    ?: DEFAULT_MAX_DIMENSION).takeIf { it > 0 }?.coerceAtMost(4096) ?: DEFAULT_MAX_DIMENSION,
                maxImageHeight = (config?.getInt(SmsManager.MMS_CONFIG_MAX_IMAGE_HEIGHT, DEFAULT_MAX_DIMENSION)
                    ?: DEFAULT_MAX_DIMENSION).takeIf { it > 0 }?.coerceAtMost(4096) ?: DEFAULT_MAX_DIMENSION,
                maxSubjectLength = (config?.getInt(SmsManager.MMS_CONFIG_SUBJECT_MAX_LENGTH, 40) ?: 40)
                    .takeIf { it > 0 }?.coerceAtMost(80) ?: 40,
            )
        }

        private const val DEFAULT_MAX_BYTES = 300 * 1024
        private const val DEFAULT_MAX_DIMENSION = 1280
    }
}
