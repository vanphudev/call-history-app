package com.antimobile.mcas.data.blocking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** DAO nội bộ cho quy tắc chặn và lịch sử app-owned. */
@Dao
interface CallBlockDao {

    // ---- Exact allow/block entries ----

    @Query("SELECT * FROM call_block_number_entries ORDER BY createdAt DESC, id DESC")
    fun observeNumberEntries(): Flow<List<CallBlockNumberEntryEntity>>

    @Query("SELECT * FROM call_block_number_entries ORDER BY createdAt DESC, id DESC")
    suspend fun getNumberEntries(): List<CallBlockNumberEntryEntity>

    @Query("SELECT * FROM call_block_number_entries WHERE enabled = 1 ORDER BY createdAt DESC, id DESC")
    suspend fun getEnabledNumberEntries(): List<CallBlockNumberEntryEntity>

    @Query("SELECT * FROM call_block_number_entries WHERE enabled = 1 AND phoneKey = :phoneKey ORDER BY action ASC, createdAt DESC, id DESC")
    suspend fun getEnabledNumberEntries(phoneKey: String): List<CallBlockNumberEntryEntity>

    @Query("SELECT * FROM call_block_number_entries WHERE id = :id")
    suspend fun getNumberEntry(id: Long): CallBlockNumberEntryEntity?

    @Query("SELECT * FROM call_block_number_entries WHERE action = :action AND phoneKey = :phoneKey LIMIT 1")
    suspend fun getNumberEntry(action: String, phoneKey: String): CallBlockNumberEntryEntity?

    @Query("SELECT COUNT(*) FROM call_block_number_entries")
    suspend fun numberEntryCount(): Int

    @Query("SELECT COUNT(*) FROM call_block_number_entries WHERE action = :action AND phoneKey = :phoneKey AND id != :exceptId")
    suspend fun numberEntrySignatureExists(action: String, phoneKey: String, exceptId: Long = -1L): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNumberEntry(entry: CallBlockNumberEntryEntity): Long

    @Update
    suspend fun updateNumberEntry(entry: CallBlockNumberEntryEntity)

    @Query("DELETE FROM call_block_number_entries WHERE id = :id")
    suspend fun deleteNumberEntry(id: Long)

    @Query("DELETE FROM call_block_number_entries WHERE action = :action AND phoneKey = :phoneKey")
    suspend fun deleteNumberEntry(action: String, phoneKey: String)

    @Query("DELETE FROM call_block_number_entries WHERE phoneKey = :phoneKey AND id != :exceptId")
    suspend fun deleteOppositeOrDuplicateNumberEntries(phoneKey: String, exceptId: Long = -1L)

    @Query("DELETE FROM call_block_number_entries")
    suspend fun deleteAllNumberEntries()

    // ---- Rules ----

    @Query("SELECT * FROM call_block_rules ORDER BY createdAt DESC, id DESC")
    fun observeRules(): Flow<List<CallBlockRuleEntity>>

    @Query("SELECT * FROM call_block_rules ORDER BY createdAt DESC, id DESC")
    suspend fun getRules(): List<CallBlockRuleEntity>

    @Query("SELECT * FROM call_block_rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<CallBlockRuleEntity>

    @Query("SELECT * FROM call_block_rules WHERE id = :id")
    suspend fun getRule(id: Long): CallBlockRuleEntity?

    @Query("SELECT COUNT(*) FROM call_block_rules")
    suspend fun ruleCount(): Int

    @Query("SELECT COUNT(*) FROM call_block_rules WHERE action = :action AND type = :type AND matchValue = :matchValue AND scope = :scope AND id != :exceptId")
    suspend fun ruleSignatureExists(
        type: String,
        matchValue: String,
        exceptId: Long = -1L,
        action: String = "block",
        scope: String = "all_visible",
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRule(rule: CallBlockRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CallBlockRuleEntity)

    @Query("DELETE FROM call_block_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM call_block_rules")
    suspend fun deleteAllRules()

    // ---- History ----

    @Query(
        """
        SELECT h.*,
               (SELECT COUNT(*) FROM call_block_history c WHERE c.phoneKey = h.phoneKey) AS blockedCountForNumber
        FROM call_block_history h
        ORDER BY h.blockedAt DESC, h.id DESC
        """
    )
    fun observeHistory(): Flow<List<CallBlockHistoryRow>>

    @Query("SELECT * FROM call_block_history ORDER BY blockedAt DESC, id DESC")
    suspend fun getHistory(): List<CallBlockHistoryEntity>

    @Query("SELECT COUNT(*) FROM call_block_history")
    suspend fun historyCount(): Int

    @Query("SELECT COUNT(*) FROM call_block_history WHERE phoneKey = :phoneKey")
    suspend fun blockCountForNumber(phoneKey: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM call_block_history
        WHERE phoneKey = :phoneKey AND blockedAt = :blockedAt AND ruleType = :ruleType AND ruleValue = :ruleValue
          AND id != :exceptId
        """
    )
    suspend fun historySignatureExists(
        phoneKey: String,
        blockedAt: Long,
        ruleType: String,
        ruleValue: String,
        exceptId: Long = -1L,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: CallBlockHistoryEntity): Long

    @Query("UPDATE call_block_history SET rawNumber = :rawNumber, phoneKey = :phoneKey WHERE id = :id")
    suspend fun updateHistoryIdentity(id: Long, rawNumber: String, phoneKey: String)

    @Query("DELETE FROM call_block_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM call_block_history")
    suspend fun deleteAllHistory()

    /** Giữ tối đa [max] bản ghi MỚI nhất để lịch sử không phình vô hạn. */
    @Query(
        """
        DELETE FROM call_block_history
        WHERE id NOT IN (
            SELECT id FROM call_block_history ORDER BY blockedAt DESC, id DESC LIMIT :max
        )
        """
    )
    suspend fun trimHistory(max: Int)
}

/** Projection của Room: cột entity + tổng lần cùng số. */
data class CallBlockHistoryRow(
    val id: Long,
    val rawNumber: String,
    val phoneKey: String,
    val blockedAt: Long,
    val ruleType: String,
    val ruleValue: String,
    val ruleScope: String,
    val consecutiveUnanswered: Int,
    val blockedCountForNumber: Int,
)
