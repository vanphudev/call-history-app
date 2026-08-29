package com.antimobile.mcas.ui.agency

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.agency.AgencyData
import com.antimobile.mcas.data.agency.AgencyDataset
import com.antimobile.mcas.data.agency.AgencyOrigin
import com.antimobile.mcas.data.agency.AgencyRepository
import com.antimobile.mcas.data.agency.AgencyResult
import com.antimobile.mcas.util.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Số mục mỗi trang (yêu cầu: phân trang 50 mục/trang). */
const val AGENCY_PAGE_SIZE = 50

/**
 * ViewModel màn DANH BẠ CƠ QUAN của một [AgencyDataset] (tỉnh × ngành): nạp dữ liệu ở luồng nền qua
 * [AgencyRepository] (PURE REMOTE: tải từ GitHub + cache 7 ngày, không seed), giữ từ khoá tìm kiếm và
 * trang hiện tại. Lọc + phân trang tính ở tầng UI (rẻ, ~vài trăm mục); VM giữ NGUỒN sự thật.
 *
 * Khi CHƯA có dữ liệu nào mà lỗi: phân biệt **mất mạng** ([noNetwork] → hiện modal nhắc bật mạng) với
 * **có mạng nhưng máy chủ lỗi** ([loadError] → màn lỗi + nút Thử lại). Nếu đã có cache (kể cả hết hạn)
 * thì repo trả cache, không rơi vào nhánh lỗi/modal.
 */
class AgencyDirectoryViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx = app.applicationContext
    private val repo = AgencyRepository(app)

    var dataset by mutableStateOf<AgencyDataset?>(null)
        private set
    var data by mutableStateOf<AgencyData?>(null)
        private set
    /** Nguồn dữ liệu đang hiển thị (mạng/cache) — để hiện chú thích nhỏ. */
    var origin by mutableStateOf<AgencyOrigin?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    /** true khi đang LÀM MỚI thủ công (đã có dữ liệu cũ hiển thị). */
    var refreshing by mutableStateOf(false)
        private set
    /** true khi nạp xong nhưng KHÔNG có dữ liệu VÀ có mạng (lỗi máy chủ) — hiện màn lỗi + Thử lại. */
    var loadError by mutableStateOf(false)
        private set
    /** true khi CHƯA có dữ liệu nào mà thiết bị KHÔNG có mạng — hiện modal nhắc kiểm tra kết nối. */
    var noNetwork by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var page by mutableStateOf(0)
        private set

    private var job: Job? = null

    /** Nạp tập [d]. Bỏ qua nếu đang mở đúng tập đó & đã có dữ liệu (giữ nguyên query/vị trí). */
    fun load(d: AgencyDataset) {
        if (dataset == d && data != null) return
        dataset = d
        data = null
        origin = null
        query = ""
        page = 0
        loadError = false
        noNetwork = false
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val res = withContext(Dispatchers.IO) { runCatching { repo.load(d, forceRefresh = false) }.getOrNull() }
            applyResult(res)
            loading = false
        }
    }

    /** Làm mới THỦ CÔNG: buộc gọi mạng (bỏ qua hạn 7 ngày). Giữ dữ liệu cũ hiển thị trong lúc tải. */
    fun refresh() {
        val d = dataset ?: return
        if (refreshing || loading) return
        job?.cancel()
        job = viewModelScope.launch {
            refreshing = true
            val res = withContext(Dispatchers.IO) { runCatching { repo.load(d, forceRefresh = true) }.getOrNull() }
            applyResult(res)
            refreshing = false
        }
    }

    /** Áp kết quả nạp: có dữ liệu → hiển thị; không & chưa có dữ liệu → phân biệt mất mạng / lỗi máy chủ. */
    private fun applyResult(res: AgencyResult?) {
        if (res != null && res.data.agencies.isNotEmpty()) {
            data = res.data
            origin = res.origin
            loadError = false
            noNetwork = false
        } else if (data == null) {
            if (NetworkUtil.isOnline(appCtx)) {
                loadError = true
                noNetwork = false
            } else {
                noNetwork = true
                loadError = false
            }
        }
        // else: đã có dữ liệu cũ + làm mới thất bại → giữ nguyên, không báo lỗi.
    }

    /** Đóng modal "không có mạng" (người dùng bấm Đóng). */
    fun dismissNoNetwork() {
        noNetwork = false
    }

    /** Đổi từ khoá tìm kiếm → về trang đầu (kết quả lọc đổi, tránh kẹt ở trang trống). */
    fun onQueryChange(q: String) {
        if (q == query) return
        query = q
        page = 0
    }

    fun clearQuery() {
        query = ""
        page = 0
    }

    fun goToPage(target: Int, pageCount: Int) {
        page = target.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    }
}
