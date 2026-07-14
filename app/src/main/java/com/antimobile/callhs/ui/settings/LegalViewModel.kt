package com.antimobile.callhs.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.legal.LegalDocument
import com.antimobile.callhs.data.legal.LegalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel màn PHÁP LÝ (Chính sách/Điều khoản): tải nội dung từ `legal.xml` trên GitHub (cache 30 ngày) ở
 * luồng nền. KHÔNG báo lỗi mạng — nếu chưa tải được (offline lần đầu) thì [doc] = null và màn chỉ hiện nút
 * "Mở trên web" (theo yêu cầu: phần pháp lý không cần thông báo mạng, chỉ cần điều hướng sang web).
 */
class LegalViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LegalRepository(app)

    var docId by mutableStateOf<String?>(null)
        private set
    var doc by mutableStateOf<LegalDocument?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    fun load(id: String) {
        if (docId == id && doc != null) return
        docId = id
        doc = null
        viewModelScope.launch {
            loading = true
            doc = withContext(Dispatchers.IO) { runCatching { repo.load(id) }.getOrNull() }
            loading = false
        }
    }
}
