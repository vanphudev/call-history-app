package com.antimobile.mcas.ui.messaging

import android.app.Application
import android.annotation.SuppressLint
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.messaging.local.MessageDraftEntity
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.model.MessagingSim
import com.antimobile.mcas.data.messaging.model.MessageTransport
import com.antimobile.mcas.data.messaging.model.MmsDownloadState
import com.antimobile.mcas.data.messaging.model.SendFailure
import com.antimobile.mcas.data.messaging.model.SendMessageResult
import com.antimobile.mcas.data.messaging.model.SmsMessageItem
import com.antimobile.mcas.data.messaging.model.SmsSegmentInfo
import com.antimobile.mcas.data.messaging.notification.MessageNotifier
import com.antimobile.mcas.data.messaging.mms.MmsCarrierLimits
import com.antimobile.mcas.data.messaging.mms.MmsDownloadCoordinator
import com.antimobile.mcas.data.messaging.mms.MmsImageProcessor
import com.antimobile.mcas.data.messaging.mms.MmsSendCoordinator
import com.antimobile.mcas.data.messaging.mms.PreparedMmsImage
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.data.messaging.sim.MessagingSimRepository
import com.antimobile.mcas.data.messaging.transport.SmsSegmentCalculator
import com.antimobile.mcas.data.messaging.transport.SmsSendCoordinator
import com.antimobile.mcas.util.SmsSettings
import com.antimobile.mcas.util.SmsText
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
    private val mmsSender = MmsSendCoordinator(context)
    private val imageProcessor = MmsImageProcessor(context)

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
    var subject by mutableStateOf("")
        private set
    var subjectVisible by mutableStateOf(false)
        private set
    var preparedImage by mutableStateOf<PreparedMmsImage?>(null)
        private set
    var processingImage by mutableStateOf(false)
        private set
    var downloadingMmsIds by mutableStateOf<Set<Long>>(emptySet())
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
    private var initialSubject = ""
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
        initialSubject: String = "",
    ) {
        resetForOpen(address, displayName, photoUri, initialBody, initialSubject)
        this.threadId = threadId
        loadConversation()
    }

    fun openAddress(address: String, initialBody: String = "", initialSubject: String = "") {
        resetForOpen(address, null, null, initialBody, initialSubject)
        this.threadId = null
        loadConversation()
    }

    private fun resetForOpen(
        address: String,
        displayName: String?,
        photoUri: String?,
        initialBody: String,
        initialSubject: String,
    ) {
        this.address = address.trim()
        this.title = displayName?.takeIf(String::isNotBlank) ?: this.address
        this.photoUri = photoUri
        this.initialBody = initialBody
        this.initialSubject = initialSubject
        messages = emptyList()
        draft = ""
        subject = ""
        subjectVisible = false
        preparedImage = null
        processingImage = false
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
                if (subject.isEmpty() && initialSubject.isNotBlank()) {
                    subject = initialSubject.take(80)
                    subjectVisible = true
                }
                initialBody = ""
                initialSubject = ""
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
                downloadingMmsIds = downloadingMmsIds.intersect(
                    data.filter { it.transport == MessageTransport.MMS && it.mmsDownloadState == MmsDownloadState.PENDING }
                        .mapTo(mutableSetOf()) { it.id }
                )
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

    fun updateSubject(value: String) {
        subject = value.take(80)
        sendFailure = null
    }

    fun toggleSubject() {
        subjectVisible = !subjectVisible
        if (!subjectVisible) subject = ""
        sendFailure = null
    }

    fun selectImage(uri: Uri) {
        if (processingImage) return
        processingImage = true
        sendFailure = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val subId = simRepository.resolve(threadId, selectedSubId)
                    ?: return@withContext Result.failure(IllegalStateException("No active SIM"))
                val limits = MmsCarrierLimits.load(context, subId)
                if (!limits.enabled) return@withContext Result.failure(UnsupportedOperationException("MMS disabled"))
                imageProcessor.prepare(uri, limits, draft.toByteArray().size + subject.toByteArray().size)
            }
            result.onSuccess { preparedImage = it; subjectVisible = true }
                .onFailure {
                    sendFailure = when (it) {
                        is UnsupportedOperationException -> SendFailure.MMS_DISABLED
                        is IllegalStateException -> SendFailure.SIM_UNAVAILABLE
                        else -> SendFailure.IMAGE_UNREADABLE
                    }
                }
            processingImage = false
        }
    }

    fun removeImage() {
        preparedImage = null
        if (subject.isBlank()) subjectVisible = false
        sendFailure = null
    }

    fun selectSim(subId: Int) {
        if (sims.none { it.subscriptionId == subId }) return
        selectedSubId = subId
        threadId?.let { id -> viewModelScope.launch(Dispatchers.IO) { simRepository.savePreferred(id, subId) } }
        preparedImage?.sourceUri?.let { selectImage(Uri.parse(it)) }
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

    fun sendMms() {
        val image = preparedImage
        if (image == null && subject.isBlank()) return
        if (sending) return
        sending = true
        val bodyToSend = draft
        val subjectToSend = subject
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val id = threadId ?: runCatching { repository.threadIdForAddress(address) }.getOrNull()
                    val resolvedSub = simRepository.resolve(id, selectedSubId)
                    if (resolvedSub == null) SendMessageResult.Failed(SendFailure.SIM_UNAVAILABLE)
                    else mmsSender.send(address, bodyToSend, subjectToSend, image, resolvedSub)
                }.getOrElse { SendMessageResult.Failed(SendFailure.PROVIDER_ERROR) }
            }
            when (result) {
                is SendMessageResult.Queued -> {
                    threadId = result.threadId
                    selectedSubId?.let { sub ->
                        withContext(Dispatchers.IO) { runCatching { simRepository.savePreferred(result.threadId, sub) } }
                    }
                    draft = ""
                    subject = ""
                    subjectVisible = false
                    preparedImage = null
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
        if (message.transport == MessageTransport.MMS && message.direction == com.antimobile.mcas.data.messaging.model.MessageDirection.INCOMING) {
            downloadMms(message.id)
        } else {
            updateDraft(message.body)
        }
    }

    fun downloadMms(providerId: Long) {
        if (providerId in downloadingMmsIds) return
        downloadingMmsIds = downloadingMmsIds + providerId
        viewModelScope.launch {
            val started = withContext(Dispatchers.IO) { runCatching { MmsDownloadCoordinator(context).start(providerId) }.getOrDefault(false) }
            if (!started) downloadingMmsIds = downloadingMmsIds - providerId
        }
    }

    fun deleteMessage(message: SmsMessageItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { repository.deleteMessage(message.id, message.transport) } }
            reloadMessages()
        }
    }

    fun deleteConversation(onDeleted: () -> Unit) {
        val id = threadId ?: return
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                runCatching { repository.deleteThread(id) }.getOrDefault(false)
            }
            if (deleted) {
                messages = emptyList()
                onDeleted()
            }
        }
    }

    private fun startObserving() {
        if (observing) return
        val sms = runCatching { resolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer) }.isSuccess
        val mms = runCatching { resolver.registerContentObserver(Telephony.Mms.CONTENT_URI, true, observer) }.isSuccess
        observing = sms || mms
    }

    private fun clearSensitiveState(keepAddress: Boolean = false) {
        loadJob?.cancel()
        messages = emptyList()
        draft = ""
        subject = ""
        subjectVisible = false
        preparedImage = null
        processingImage = false
        downloadingMmsIds = emptySet()
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
