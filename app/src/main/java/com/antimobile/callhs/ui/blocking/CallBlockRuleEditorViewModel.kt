package com.antimobile.callhs.ui.blocking

import android.Manifest
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.blocking.CallBlockRule
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.SaveBlockRuleResult
import com.antimobile.callhs.data.contacts.Contact
import com.antimobile.callhs.data.contacts.ContactsRepository
import com.antimobile.callhs.util.ContactsSignal
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** VM cho màn tạo/sửa một quy tắc chặn, giữ dữ liệu Room ngoài Compose UI. */
class CallBlockRuleEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CallBlockRepository(app)
    private val contactsRepo = ContactsRepository(app.contentResolver)

    var rule by mutableStateOf<CallBlockRule?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var contactsLoading by mutableStateOf(false)
        private set
    var contactsLoaded by mutableStateOf(false)
        private set
    var contactsLoadFailed by mutableStateOf(false)
        private set
    var hasContactsPermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_CONTACTS))
        private set

    private var bound = false
    private var ruleId: Long? = null
    private var contactsJob: Job? = null

    init {
        ContactsSignal.observe(viewModelScope) {
            if (contactsLoaded) loadContacts()
        }
    }

    fun bind(id: Long?) {
        if (bound) return
        bound = true
        ruleId = id
        viewModelScope.launch {
            rule = id?.let { repo.getRule(it) }
            loaded = true
        }
    }

    fun save(
        type: CallBlockRuleType,
        value: String,
        enabled: Boolean,
        scope: CallBlockScope,
        action: CallBlockAction,
        onDone: (SaveBlockRuleResult) -> Unit,
    ) {
        viewModelScope.launch {
            onDone(repo.saveRule(ruleId, type, value, enabled, action = action, scope = scope))
        }
    }

    /** Nạp danh bạ chỉ khi picker cần dùng; mọi truy vấn provider chạy trên IO và lỗi luôn fail-safe. */
    fun loadContacts() {
        hasContactsPermission = hasPermission(getApplication(), Manifest.permission.READ_CONTACTS)
        if (!hasContactsPermission) return
        if (contactsJob?.isActive == true && !contactsLoaded) return
        ContactsSignal.ensureRegistered(getApplication())
        contactsJob?.cancel()
        contactsJob = viewModelScope.launch {
            contactsLoading = true
            val result = withContext(Dispatchers.IO) { runCatching { contactsRepo.loadAll() } }
            result.onSuccess {
                contacts = it
                contactsLoaded = true
                contactsLoadFailed = false
            }.onFailure {
                contactsLoadFailed = true
            }
            contactsLoading = false
        }
    }

    /** Dùng sau khi cấp quyền hoặc quay lại từ Settings để đồng bộ permission + dữ liệu mới nhất. */
    fun refreshContacts() {
        hasContactsPermission = hasPermission(getApplication(), Manifest.permission.READ_CONTACTS)
        if (hasContactsPermission) loadContacts()
    }
}
