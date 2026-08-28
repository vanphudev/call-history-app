package com.antimobile.callhs.data.messaging.local

import androidx.room.Entity

@Entity(tableName = "message_drafts", primaryKeys = ["draftKey"])
data class MessageDraftEntity(
    val draftKey: String,
    val threadId: Long?,
    val address: String,
    val body: String,
    val updatedAt: Long,
)

@Entity(tableName = "conversation_preferences", primaryKeys = ["threadId"])
data class ConversationPreferenceEntity(
    val threadId: Long,
    val preferredSubId: Int?,
    val updatedAt: Long,
)

@Entity(tableName = "sms_part_attempts", primaryKeys = ["attemptId", "partIndex"])
data class SmsPartAttemptEntity(
    val attemptId: String,
    val partIndex: Int,
    val totalParts: Int,
    val providerId: Long,
    val threadId: Long,
    val address: String,
    val subscriptionId: Int,
    val sentResult: Int?,
    val delivered: Boolean,
    val errorCode: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "inbound_fingerprints", primaryKeys = ["fingerprint"])
data class InboundFingerprintEntity(
    val fingerprint: String,
    val createdAt: Long,
)

