package com.antimobile.mcas.data.messaging.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessagingDao {
    @Query("SELECT * FROM message_drafts WHERE draftKey = :key")
    suspend fun getDraft(key: String): MessageDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: MessageDraftEntity)

    @Query("DELETE FROM message_drafts WHERE draftKey = :key")
    suspend fun deleteDraft(key: String)

    @Query("SELECT * FROM conversation_preferences WHERE threadId = :threadId")
    suspend fun getConversationPreference(threadId: Long): ConversationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConversationPreference(value: ConversationPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<SmsPartAttemptEntity>)

    @Query("UPDATE sms_part_attempts SET sentResult = :resultCode, errorCode = :errorCode, updatedAt = :now WHERE attemptId = :attemptId AND partIndex = :partIndex")
    suspend fun markPartSent(attemptId: String, partIndex: Int, resultCode: Int, errorCode: Int?, now: Long)

    @Query("UPDATE sms_part_attempts SET delivered = 1, updatedAt = :now WHERE attemptId = :attemptId AND partIndex = :partIndex")
    suspend fun markPartDelivered(attemptId: String, partIndex: Int, now: Long)

    @Query("SELECT * FROM sms_part_attempts WHERE attemptId = :attemptId ORDER BY partIndex")
    suspend fun getParts(attemptId: String): List<SmsPartAttemptEntity>

    @Query("DELETE FROM sms_part_attempts WHERE updatedAt < :before")
    suspend fun deleteOldAttempts(before: Long)

    @Query("SELECT COUNT(*) FROM inbound_fingerprints WHERE fingerprint = :fingerprint")
    suspend fun hasFingerprint(fingerprint: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFingerprint(value: InboundFingerprintEntity): Long

    @Query("DELETE FROM inbound_fingerprints WHERE createdAt < :before")
    suspend fun deleteOldFingerprints(before: Long)

    @Query("SELECT * FROM mms_transfers WHERE providerId = :providerId")
    suspend fun getMmsTransfer(providerId: Long): MmsTransferEntity?

    @Query("SELECT * FROM mms_transfers WHERE transactionId = :transactionId")
    suspend fun getMmsTransferByTransaction(transactionId: String): MmsTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMmsTransfer(value: MmsTransferEntity)

    @Query("UPDATE mms_transfers SET state = :state, tempFileName = :tempFileName, attemptCount = attemptCount + 1, resultCode = NULL, httpStatus = NULL, updatedAt = :now WHERE providerId = :providerId")
    suspend fun markMmsTransferStarted(providerId: Long, state: String, tempFileName: String, now: Long)

    @Query("UPDATE mms_transfers SET state = :state, resultCode = :resultCode, httpStatus = :httpStatus, tempFileName = NULL, updatedAt = :now WHERE providerId = :providerId")
    suspend fun finishMmsTransfer(providerId: Long, state: String, resultCode: Int?, httpStatus: Int?, now: Long)

    @Query("DELETE FROM mms_transfers WHERE updatedAt < :before")
    suspend fun deleteOldMmsTransfers(before: Long)
}
