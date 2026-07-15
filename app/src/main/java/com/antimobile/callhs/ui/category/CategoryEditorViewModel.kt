package com.antimobile.callhs.ui.category

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.contacts.ContactsRepository
import com.antimobile.callhs.data.local.AddMemberResult
import com.antimobile.callhs.data.local.Category
import com.antimobile.callhs.data.local.CategoryMember
import com.antimobile.callhs.data.local.CategoryRepository
import com.antimobile.callhs.util.ContactsSignal
import com.antimobile.callhs.util.PhoneKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Một dòng số điện thoại trong nhóm, đã giải danh bạ (nếu số nằm trong danh bạ). */
data class MemberRow(
    val rawNumber: String,
    val phoneKey: String,
    val addedAt: Long,
    val contactName: String?,
    val photoUri: String?,
) {
    val isNamed: Boolean get() = !contactName.isNullOrBlank()
    val displayLabel: String get() = contactName?.takeIf { it.isNotBlank() } ?: rawNumber
}

/**
 * VM cho màn soạn nhóm. Với nhóm ĐANG SỬA: nạp thông tin nhóm + theo dõi danh sách thành viên (Room Flow)
 * và giải TÊN/ẢNH danh bạ theo số (giống CallListScreen) — refresh khi danh bạ đổi qua [ContactsSignal].
 * Với nhóm TẠO MỚI (id ≤ 0): chỉ để lưu (create) khi bấm Lưu.
 */
class CategoryEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CategoryRepository(app)
    private val contactsRepo = ContactsRepository(app.contentResolver)

    var category by mutableStateOf<Category?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set
    var members by mutableStateOf<List<MemberRow>>(emptyList())
        private set

    private var categoryId = 0L
    private var bound = false
    private var rawMembers: List<CategoryMember> = emptyList()
    private var contactIndex: Map<String, Pair<String?, String?>> = emptyMap()
    private var membersJob: Job? = null
    init { ContactsSignal.observe(viewModelScope) { loadContacts() } }

    fun bind(id: Long) {
        if (bound) return
        bound = true
        categoryId = id
        viewModelScope.launch {
            if (id > 0L) category = repo.getCategory(id)
            loaded = true
        }
        if (id > 0L) {
            membersJob = viewModelScope.launch {
                repo.observeMembers(id).collect { rawMembers = it; rebuild() }
            }
        }
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            ContactsSignal.ensureRegistered(getApplication())
            val idx = withContext(Dispatchers.IO) {
                runCatching {
                    val map = HashMap<String, Pair<String?, String?>>()
                    contactsRepo.loadAll().forEach { c ->
                        c.phones.forEach { p ->
                            val key = PhoneKey.of(p.number)
                            if (key.isNotEmpty()) map.putIfAbsent(key, c.displayName to c.photoUri)
                        }
                    }
                    map
                }.getOrDefault(emptyMap())
            }
            contactIndex = idx
            rebuild()
        }
    }

    private fun rebuild() {
        members = rawMembers.map { m ->
            val hit = contactIndex[m.phoneKey]
            MemberRow(m.rawNumber, m.phoneKey, m.addedAt, hit?.first, hit?.second)
        }
    }

    fun removeMember(phoneKey: String) {
        viewModelScope.launch { repo.removeMember(categoryId, phoneKey) }
    }

    /**
     * Lưu nhóm. Nhóm mới → tạo, trả id (null nếu vượt cap tối đa 5). Nhóm cũ → cập nhật, trả id hiện tại.
     * Với nhóm mặc định, [name] truyền vào là tên gốc (không đổi) để giữ tên localize.
     */
    fun save(name: String, description: String, iconKey: String, colorArgb: Long, onDone: (Long?) -> Unit) {
        viewModelScope.launch {
            if (categoryId > 0L) {
                repo.updateCategory(categoryId, name, description, iconKey, colorArgb)
                onDone(categoryId)
            } else {
                val id = repo.createCategory(name, description, iconKey, colorArgb)
                if (id != null) categoryId = id
                onDone(id)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
