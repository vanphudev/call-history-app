package com.antimobile.mcas.ui.messaging

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
import com.antimobile.mcas.data.messaging.model.ConversationSummary
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.util.ContactsSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationListViewModel(app: Application) : AndroidViewModel(app) {
    @SuppressLint("StaticFieldLeak")
    private val context = app.applicationContext
    private val resolver = app.contentResolver
    private val repository = TelephonyMessageRepository(context)

    var conversations by mutableStateOf<List<ConversationSummary>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set

    private var loadJob: Job? = null
    private var debounceJob: Job? = null
    private var observing = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                delay(220L)
                load()
            }
        }
    }

    init {
        ContactsSignal.observe(viewModelScope) {
            repository.clearContactCache()
            load()
        }
    }

    fun refreshCapability() {
        if (SmsRole.canRead(context)) load() else clearSensitiveState()
    }

    fun load() = fetch(userInitiated = false)

    fun refresh() = fetch(userInitiated = true)

    private fun fetch(userInitiated: Boolean) {
        if (!SmsRole.canRead(context)) {
            clearSensitiveState()
            return
        }
        startObserving()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (userInitiated) refreshing = true else loading = !loaded
            val result = withContext(Dispatchers.IO) { runCatching { repository.loadConversations() } }
            result.onSuccess {
                conversations = it
                loaded = true
                loadFailed = false
            }.onFailure { loadFailed = true }
            loading = false
            refreshing = false
        }
    }

    fun markRead(threadId: Long) = mutate { repository.markThreadRead(threadId) }

    fun markUnread(threadId: Long) = mutate { repository.markThreadUnread(threadId) }

    fun deleteConversation(threadId: Long) = mutate { repository.deleteThread(threadId) }

    private fun mutate(block: () -> Unit) {
        if (!SmsRole.isHeld(context)) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching(block) }
            load()
        }
    }

    private fun startObserving() {
        if (observing) return
        val sms = runCatching { resolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer) }.isSuccess
        val mms = runCatching { resolver.registerContentObserver(Telephony.Mms.CONTENT_URI, true, observer) }.isSuccess
        observing = sms || mms
    }

    private fun clearSensitiveState() {
        loadJob?.cancel()
        conversations = emptyList()
        loaded = false
        loading = false
        refreshing = false
        loadFailed = false
        if (observing) runCatching { resolver.unregisterContentObserver(observer) }
        observing = false
    }

    override fun onCleared() {
        if (observing) runCatching { resolver.unregisterContentObserver(observer) }
        super.onCleared()
    }
}
