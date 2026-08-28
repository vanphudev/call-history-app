package com.antimobile.callhs.data.messaging.transport

import android.app.Activity
import android.content.Context
import com.antimobile.callhs.data.messaging.local.MessagingDatabase
import com.antimobile.callhs.data.messaging.provider.TelephonyMessageRepository

object SmsCallbackProcessor {
    suspend fun onSent(
        context: Context,
        attemptId: String,
        partIndex: Int,
        resultCode: Int,
        errorCode: Int?,
    ) {
        val dao = MessagingDatabase.get(context).messagingDao()
        dao.markPartSent(attemptId, partIndex, resultCode, errorCode, System.currentTimeMillis())
        val parts = dao.getParts(attemptId)
        val resolution = SmsPartResultReducer.sent(parts.map { it.sentResult }, Activity.RESULT_OK)
        if (resolution is SmsPartSendResolution.Pending) return
        val providerId = parts.first().providerId
        runCatching {
            val provider = TelephonyMessageRepository(context)
            when (resolution) {
                SmsPartSendResolution.Success -> provider.updateOutgoingSent(providerId, delivered = parts.all { it.delivered })
                is SmsPartSendResolution.Failure -> {
                    val failedPart = parts[resolution.partIndex]
                    provider.updateOutgoingFailed(providerId, failedPart.errorCode ?: failedPart.sentResult)
                }
                SmsPartSendResolution.Pending -> Unit
            }
        }
    }

    suspend fun onDelivered(context: Context, attemptId: String, partIndex: Int, resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) return
        val dao = MessagingDatabase.get(context).messagingDao()
        dao.markPartDelivered(attemptId, partIndex, System.currentTimeMillis())
        val parts = dao.getParts(attemptId)
        if (parts.isNotEmpty() && parts.all { it.delivered }) {
            runCatching { TelephonyMessageRepository(context).updateOutgoingDelivered(parts.first().providerId) }
        }
    }
}
