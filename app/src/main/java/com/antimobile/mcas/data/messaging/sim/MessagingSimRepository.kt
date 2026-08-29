package com.antimobile.mcas.data.messaging.sim

import android.content.Context
import android.telephony.SubscriptionManager
import com.antimobile.mcas.data.messaging.local.ConversationPreferenceEntity
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.model.MessagingSim
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.util.SimInfo

class MessagingSimRepository(private val context: Context) {
    private val dao = MessagingDatabase.get(context).messagingDao()
    private val provider = TelephonyMessageRepository(context)

    fun activeSims(): List<MessagingSim> = SimInfo.activeSims(context).map {
        MessagingSim(
            subscriptionId = it.subscriptionId,
            slotIndex = it.slotIndex,
            label = it.simLabel,
            displayName = it.displayName,
            carrier = it.carrier,
        )
    }

    suspend fun preferredForThread(threadId: Long): Int? = dao.getConversationPreference(threadId)?.preferredSubId

    suspend fun savePreferred(threadId: Long, subId: Int) {
        dao.saveConversationPreference(ConversationPreferenceEntity(threadId, subId, System.currentTimeMillis()))
    }

    suspend fun resolve(threadId: Long?, explicitlySelected: Int?): Int? {
        val active = activeSims().mapTo(HashSet()) { it.subscriptionId }
        explicitlySelected?.takeIf(active::contains)?.let { return it }
        if (threadId != null) {
            preferredForThread(threadId)?.takeIf(active::contains)?.let { return it }
            runCatching { provider.latestIncomingSubId(threadId) }.getOrNull()?.takeIf(active::contains)?.let { return it }
        }
        val systemDefault = SubscriptionManager.getDefaultSmsSubscriptionId()
        systemDefault.takeIf(active::contains)?.let { return it }
        return active.singleOrNull()
    }
}

