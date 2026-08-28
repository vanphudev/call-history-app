package com.antimobile.callhs.ui.messaging

import android.app.Application
import android.annotation.SuppressLint
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.messaging.local.MessageDraftEntity
import com.antimobile.callhs.data.messaging.local.MessagingDatabase
import com.antimobile.callhs.data.messaging.model.MessagingSim
import com.antimobile.callhs.data.messaging.model.SendFailure
import com.antimobile.callhs.data.messaging.model.SendMessageResult
import com.antimobile.callhs.data.messaging.model.SmsMessageItem
import com.antimobile.callhs.data.messaging.model.SmsSegmentInfo
import com.antimobile.callhs.data.messaging.notification.MessageNotifier
import com.antimobile.callhs.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.callhs.data.messaging.role.SmsRole
import com.antimobile.callhs.data.messaging.sim.MessagingSimRepository
import com.antimobile.callhs.data.messaging.transport.SmsSegmentCalculator
import com.antimobile.callhs.data.messaging.transport.SmsSendCoordinator
import com.antimobile.callhs.util.SmsSettings
import com.antimobile.callhs.util.SmsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationViewModel(app: Application) : AndroidViewModel(app) {
    @SuppressLint("StaticFieldLeak")
    private val context = app.applicationContext
    private val resolver = app.contentResolver
    private val repository = TelephonyMessageRepository(context)
    private val simRepository = MessagingSimRepository(context)
    private val dao = MessagingDatabase.get(context).messagingDao()
    private val sender = SmsSendCoordinator(context)

    var threadId by mutableStateOf<Long?>(null)
        private set
    var address by mutableStateOf("")
        private set
    var title by mutableStateOf("")
        private set
    var photoUri by mutableStateOf<String?>(null)
        private set
    var messages by mutableStateOf<List<SmsMessageItem>>(emptyList())
        private set
    var draft by mutableStateOf("")
        private set
    var sims by mutableStateOf<List<MessagingSim>>(emptyList())
        private set
    var selectedSubId by mutableStateOf<Int?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var sendFailure by mutableStateOf<SendFailure?>(null)
        private set

    private var initialBody = ""
    private var loadJob: Job? = null
    private var draftJob: Job? = null
    private var debounceJob: Job? = null
    private var observing = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch { delay(180L); reloadMessages() }
        }
    }

    fun openThread(
        threadId: Long,
        address: String,
        displayName: String? = null,
        photoUri: String? = null,
        initialBody: String = "",
    ) {
        resetForOpen(address, displayName, photoUri, initialBody)
        this.threadId = threadId
        loadConversation()
    }

    fun openAddress(address: String, initialBody: String = "") {
        resetForOpen(address, null, null, initialBody)
        this.threadId = null
        loadConversation()
    }

    private fun resetForOpen(address: String, displayName: String?, photoUri: String?, initialBody: String) {
        this.address = address.trim()
        this.title = displayName?.takeIf(String::isNotBlank) ?: this.address
        this.photoUri = photoUri
        this.initialBody = initialBody
        messages = emptyList()
        draft = ""
        selectedSubId = null
        sendFailure = null
        loadFailed = false
    }

    fun refreshCapability() {
        if (SmsRole.canRead(context)) loadConversation() else clearSensitiveState()
    }

    private fun loadConversation() {
        if (!SmsRole.canRead(context) || address.isBlank()) {
            clearSensitiveState(keepAddress = true)
            return
        }
        startObserving()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val resolvedThread = threadId ?: repository.threadIdForAddress(address)
                    val identity = repository.identityForAddress(address)
                    val loadedMessages = repository.loadMessages(resolvedThread)
                    repository.markThreadRead(resolvedThread)
                    val activeSims = simRepository.activeSims()
                    val selected = simRepository.resolve(resolvedThread, selectedSubId)
                    val threadDraft = dao.getDraft(draftKey(resolvedThread, address))?.body
                    val addressDraft = dao.getDraft(draftKey(null, address))?.body
                    val storedDraft = threadDraft ?: addressDraft.orEmpty()
                    LoadedConversation(resolvedThread, identity?.name, identity?.photoUri, loadedMessages, activeSims, selected, storedDraft)
                }
            }
            result.onSuccess { loaded ->
                threadId = loaded.threadId
                title = loaded.name?.takeIf(String::isNotBlank) ?: address
                photoUri = loaded.photoUri
                messages = loaded.messages
                sims = loaded.sims
                selectedSubId = loaded.selectedSubId
                if (draft.isEmpty()) draft = initialBody.ifBlank { loaded.draft }
                initialBody = ""
                MessageNotifier.cancelThread(context, loaded.threadId)
                loadFailed = false
            }.onFailure { loadFailed = true }
            loading = false
        }
    }

    private fun reloadMessages() {
        val id = threadId ?: return
        if (!SmsRole.canRead(context)) return
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { runCatching { repository.loadMessages(id) }.getOrNull() }
            if (data != null) {
                messages = data
                withContext(Dispatchers.IO) { runCatching { repository.markThreadRead(id) } }
                MessageNotifier.cancelThread(context, id)
            }
        }
    }

    fun updateDraft(value: String) {
        draft = value
        sendFailure = null
        val key = draftKey(threadId, address)
        val addressKey = draftKey(null, address)
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(250L)
            withContext(Dispatchers.IO) {
                if (value.isBlank()) dao.deleteDraft(key)
                else dao.saveDraft(MessageDraftEntity(key, threadId, address, value, System.currentTimeMillis()))
                if (addressKey != key) dao.deleteDraft(addressKey)
            }
        }
    }

    fun selectSim(subId: Int) {
        if (sims.none { it.subscriptionId == subId }) return
        selectedSubId = subId
        threadId?.let { id -> viewModelScope.launch(Dispatchers.IO) { simRepository.savePreferred(id, subId) } }
    }

    fun segmentInfo(): SmsSegmentInfo {
        val actual = if (SmsSettings.isRemoveDiacritics(context)) SmsText.toGsm7(draft) else draft
        return SmsSegmentCalculator.calculate(actual)
    }

    fun send() {
        if (sending || draft.isBlank()) return
        val bodyToSend = draft
        sending = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val id = threadId ?: runCatching { repository.threadIdForAddress(address) }.getOrNull()
                    val resolvedSub = simRepository.resolve(id, selectedSubId)
                    if (resolvedSub == null) SendMessageResult.Failed(SendFailure.SIM_UNAVAILABLE)
                    else sender.send(address, bodyToSend, resolvedSub)
                }.getOrElse { SendMessageResult.Failed(SendFailure.PROVIDER_ERROR) }
            }
            when (result) {
                is SendMessageResult.Queued -> {
                    threadId = result.threadId
                    selectedSubId?.let { sub ->
                        withContext(Dispatchers.IO) { runCatching { simRepository.savePreferred(result.threadId, sub) } }
                    }
                    draft = ""
                    withContext(Dispatchers.IO) {
                        runCatching {
                            dao.deleteDraft(draftKey(result.threadId, address))
                            dao.deleteDraft(draftKey(null, address))
                        }
                    }
                    reloadMessages()
                }
                is SendMessageResult.Failed -> sendFailure = result.reason
            }
            sending = false
        }
    }

    fun retry(message: SmsMessageItem) {
        updateDraft(message.body)
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.deleteMessage(messageId) } }
            reloadMessages()
        }
    }

    private fun startObserving() {
        if (observing) return
        runCatching { resolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer) }
            .onSuccess { observing = true }
    }

    private fun clearSensitiveState(keepAddress: Boolean = false) {
        loadJob?.cancel()
        messages = emptyList()
        draft = ""
        sims = emptyList()
        selectedSubId = null
        loading = false
        loadFailed = false
        if (!keepAddress) {
            threadId = null
            address = ""
            title = ""
            photoUri = null
        }
        if (observing) runCatching { resolver.unregisterContentObserver(observer) }
        observing = false
    }

    override fun onCleared() {
        if (observing) runCatching { resolver.unregisterContentObserver(observer) }
        super.onCleared()
    }

    private data class LoadedConversation(
        val threadId: Long,
        val name: String?,
        val photoUri: String?,
        val messages: List<SmsMessageItem>,
        val sims: List<MessagingSim>,
        val selectedSubId: Int?,
        val draft: String,
    )

    private fun draftKey(threadId: Long?, address: String): String =
        threadId?.let { "thread:$it" } ?: "address:${address.trim()}"
}
