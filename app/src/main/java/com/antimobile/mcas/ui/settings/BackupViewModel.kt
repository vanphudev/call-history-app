package com.antimobile.mcas.ui.settings

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.backup.BackupError
import com.antimobile.mcas.data.backup.BackupManager
import com.antimobile.mcas.data.backup.BackupSection
import com.antimobile.mcas.data.backup.ImportReport
import com.antimobile.mcas.data.backup.MergeMode
import com.antimobile.mcas.data.backup.ParsedBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel cho màn SAO LƯU & KHÔI PHỤC — giữ TRẠNG THÁI cấu hình (mục chọn, chế độ gộp, file đã phân tích)
 * vượt qua xoay màn (Activity dựng lại) và chạy phần đọc/ghi file + Room off-main. UI (BackupScreen) tạo
 * launcher SAF rồi đẩy [Uri] vào đây.
 *
 * Vì [exportSel]/[restoreSel]/[mode] nằm ở đây (không phải `remember` trong composable), khi xoay màn cấu
 * hình khôi phục KHÔNG bị đặt lại về mặc định trong khi [parsed] vẫn còn — tránh việc "làm một đằng, chạy
 * một nẻo".
 */
class BackupViewModel(app: Application) : AndroidViewModel(app) {

    /** Đang xuất/khôi phục (khoá nút, hiện "Đang…"). */
    var busy by mutableStateOf(false)
        private set

    /** File sao lưu đã phân tích để chuẩn bị khôi phục; null = chưa chọn file. */
    var parsed by mutableStateOf<ParsedBackup?>(null)
        private set

    /** Tên file người dùng đã chọn (hiển thị). */
    var pickedFileName by mutableStateOf<String?>(null)
        private set

    /** Xuất file thành công → hiện hộp thoại xác nhận. */
    var exportSucceeded by mutableStateOf(false)
        private set

    /** Kết quả khôi phục gần nhất → hiện hộp thoại tóm tắt. */
    var report by mutableStateOf<ImportReport?>(null)
        private set

    /** Lỗi gần nhất (ánh xạ chuỗi ở UI). */
    var error by mutableStateOf<BackupError?>(null)
        private set

    /** Mục chọn để XUẤT — mặc định chọn hết. Bền qua xoay màn. */
    val exportSel = mutableStateMapOf<BackupSection, Boolean>().apply {
        BackupSection.entries.forEach { put(it, true) }
    }

    /** Mục chọn để KHÔI PHỤC — điền theo mục thực có trong file khi phân tích xong. */
    val restoreSel = mutableStateMapOf<BackupSection, Boolean>()

    /** Chế độ khôi phục đang chọn (mặc định an toàn = [MergeMode.ADD]). */
    var mode by mutableStateOf(MergeMode.ADD)

    val exportSelection: Set<BackupSection> get() = exportSel.filterValues { it }.keys
    val restoreSelection: Set<BackupSection> get() = restoreSel.filterValues { it }.keys

    /** Xuất các mục đã chọn ra [uri] (đã do người dùng chọn qua SAF CreateDocument). */
    fun export(uri: Uri) {
        if (busy) return
        val sections = exportSelection
        if (sections.isEmpty()) {
            error = BackupError.NOTHING_SELECTED
            return
        }
        val context = getApplication<Application>()
        viewModelScope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = BackupManager.buildJson(context, sections)
                    BackupManager.writeText(context, uri, json)
                }.getOrDefault(false)
            }
            busy = false
            if (ok) exportSucceeded = true else error = BackupError.WRITE_FAILED
        }
    }

    private sealed interface InspectOutcome {
        data class Ok(val parsed: ParsedBackup) : InspectOutcome
        data object ReadFailed : InspectOutcome
        data object Invalid : InspectOutcome
        data object Empty : InspectOutcome
    }

    /** Đọc + phân tích file [uri] (SAF OpenDocument) để hiện tuỳ chọn khôi phục. */
    fun inspect(uri: Uri, displayName: String?) {
        if (busy) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            busy = true
            val outcome = withContext(Dispatchers.IO) {
                val text = BackupManager.readText(context, uri) ?: return@withContext InspectOutcome.ReadFailed
                val pb = BackupManager.parse(text) ?: return@withContext InspectOutcome.Invalid
                if (pb.present.isEmpty()) InspectOutcome.Empty else InspectOutcome.Ok(pb)
            }
            busy = false
            when (outcome) {
                is InspectOutcome.Ok -> {
                    parsed = outcome.parsed
                    pickedFileName = displayName
                    restoreSel.clear()
                    outcome.parsed.present.forEach { restoreSel[it] = true }
                }
                // Mọi kết quả KHÔNG thành công đều dọn file đang giữ để UI không hiển thị file cũ đằng sau lỗi.
                InspectOutcome.ReadFailed -> { clearPickedFile(); error = BackupError.READ_FAILED }
                InspectOutcome.Invalid -> { clearPickedFile(); error = BackupError.INVALID_FILE }
                InspectOutcome.Empty -> { clearPickedFile(); error = BackupError.EMPTY_BACKUP }
            }
        }
    }

    /** Khôi phục các mục đã chọn từ file đã phân tích theo [restoreMode]. */
    fun restore(restoreMode: MergeMode) {
        if (busy) return
        val source = parsed ?: return
        val sections = restoreSelection
        if (sections.isEmpty()) {
            error = BackupError.NOTHING_SELECTED
            return
        }
        val context = getApplication<Application>()
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                runCatching { BackupManager.restore(context, source, sections, restoreMode) }.getOrNull()
            }
            busy = false
            if (result != null) report = result else error = BackupError.READ_FAILED
        }
    }

    fun setSection(section: BackupSection, forExport: Boolean, checked: Boolean) {
        (if (forExport) exportSel else restoreSel)[section] = checked
    }

    fun clearPickedFile() {
        parsed = null
        pickedFileName = null
        restoreSel.clear()
    }

    fun dismissExportSuccess() {
        exportSucceeded = false
    }

    fun dismissReport() {
        report = null
    }

    fun dismissError() {
        error = null
    }
}
