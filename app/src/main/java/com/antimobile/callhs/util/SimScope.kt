package com.antimobile.callhs.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.antimobile.callhs.data.model.CallEntry

/**
 * SỔ ĐĂNG KÝ SIM — nguồn DUY NHẤT để ánh xạ PHONE_ACCOUNT_ID ↔ nhãn "SIM 1"/"SIM 2" cho TOÀN app.
 *
 * QUAN TRỌNG (đã sửa): nhãn SIM suy từ các SIM ĐANG LẮP THẬT ([SimInfo.activeSims]) — GIỐNG hệt cách màn Cước
 * ([CostStatsScreen]) và trình gọi mặc định của máy làm — CHỨ KHÔNG suy từ mọi PHONE_ACCOUNT_ID có trong nhật
 * ký. Lý do: máy có thể còn cuộc gọi CŨ của SIM đã tháo (khay vật lý / eSIM cũ) với account-id khác; nếu đếm
 * theo nhật ký thì các SIM cũ đó hiện thành "SIM 3"/"SIM 4" dù máy chỉ đang lắp 2 SIM. Chỉ cuộc gọi có
 * account-id KHỚP một SIM đang lắp mới được gán nhãn; cuộc của SIM cũ → simLabel = null (ẩn khỏi mọi tab SIM),
 * đúng như máy: cuộc từ SIM không trùng SIM đang lắp thì không hiện SIM nào.
 *
 * Cần READ_PHONE_STATE (qua [SimInfo]). Chưa cấp / không có SIM → danh sách RỖNG ⇒ không tab SIM nào hiện (không
 * đoán bừa). Khớp account-id theo CẢ subId lẫn ICCID (một số máy ghi ICCID) để bền hơn.
 */
object SimRegistry {

    /** Một SIM ĐANG LẮP: nhãn theo THỨ TỰ khe ("SIM 1".."SIM N"), các account-id có thể khớp, và nhà mạng. */
    data class Sim(
        val label: String,
        val accountIds: List<String>,  // subId (chuỗi) + ICCID nếu có — dùng khớp PHONE_ACCOUNT_ID
        val carrier: String?
    )

    // Ảnh chụp các SIM đang lắp. Snapshot-state → UI (thẻ Cài đặt, thanh lọc) tự hiện/ẩn khi SIM đổi.
    private var simsState by mutableStateOf<List<Sim>>(emptyList())

    /** Các SIM đang lắp (đã đánh nhãn SIM 1..N theo thứ tự khe). */
    val sims: List<Sim> get() = simsState

    /** Nhãn SIM đang lắp: ["SIM 1","SIM 2",…]. Rỗng nếu chưa cấp quyền / không có SIM. */
    val labels: List<String> get() = simsState.map { it.label }

    /**
     * Máy đang lắp TỪ 2 SIM trở lên? — nguồn DUY NHẤT quyết định có HIỂN THỊ nhãn "SIM n" hay không.
     * Máy chỉ 1 SIM (hoặc chưa cấp READ_PHONE_STATE / không có SIM) → KHÔNG cần phân biệt SIM nên KHÔNG hiện
     * tag "SIM 1" (dễ khiến người dùng hiểu nhầm là máy có nhiều SIM). Chỉ ảnh hưởng HIỂN THỊ — việc gán/khớp
     * [CallEntry.simLabel] theo SIM đang lắp và lọc theo phạm vi SIM vẫn giữ nguyên (xem [SimScope]).
     */
    val multiSim: Boolean get() = simsState.size >= 2

    /** Map account-id → "SIM n" (chỉ SIM đang lắp) để gán [CallEntry.simLabel]. Id lạ (SIM cũ) → không có khoá. */
    fun labelMap(): Map<String, String> {
        val m = HashMap<String, String>()
        simsState.forEach { s -> s.accountIds.forEach { m[it] = s.label } }
        return m
    }

    /** Các account-id của SIM ứng với [label] (để lọc trong truy vấn SQL). Rỗng nếu nhãn không thuộc SIM đang lắp. */
    fun accountIdsForLabel(label: String): List<String> =
        simsState.firstOrNull { it.label == label }?.accountIds ?: emptyList()

    /** Nhà mạng của SIM ứng với [label] (nếu đọc được); null nếu không rõ. */
    fun carrierForLabel(label: String): String? =
        simsState.firstOrNull { it.label == label }?.carrier

    /**
     * Làm mới từ các SIM ĐANG LẮP ([SimInfo.activeSims], đã sort theo khe). Đánh nhãn THEO THỨ TỰ: SIM đang
     * hoạt động thứ i → "SIM ${i+1}" (khớp [SimInfo.readDeviceSims] "1 SIM = SIM 1"). Rẻ (đọc SubscriptionManager)
     * nên gọi mỗi lần nạp nhật ký để bám kịp khi cắm/tháo SIM. Chưa cấp READ_PHONE_STATE → activeSims rỗng → rỗng.
     */
    fun refresh(context: Context) {
        val active = SimInfo.activeSims(context) // sorted by slotIndex; [] nếu chưa cấp quyền / không SIM
        val mapped = active.mapIndexed { i, s ->
            Sim(
                label = "SIM ${i + 1}",
                accountIds = buildList {
                    add(s.subscriptionId.toString())
                    s.iccId?.takeIf { it.isNotBlank() }?.let { add(it) }
                },
                carrier = s.carrier
            )
        }
        if (mapped != simsState) simsState = mapped
    }
}

/**
 * PHẠM VI SIM TOÀN APP — người dùng chọn ở Cài đặt để CẢ ứng dụng (danh sách, thống kê, chi tiết, cước) chỉ
 * tính & hiển thị cuộc gọi của MỘT SIM. `null` = tất cả SIM (mặc định).
 *
 * Đồng bộ cách làm với [com.antimobile.callhs.ui.theme.ThemeSettings] / [com.antimobile.callhs.i18n.LanguageSettings]:
 * SharedPreferences + snapshot-state để đổi là cả app nạp lại NGAY. Lọc được ÁP TẠI GỐC (một chỗ duy nhất:
 * [com.antimobile.callhs.data.repository.CallLogRepository]) nên mọi màn tự tuân theo, không phải sửa từng màn.
 *
 * Gọi [init] một lần sớm ở MainActivity.onCreate (như các cài đặt khác) để khung hình đầu đã đúng phạm vi.
 */
object SimScope {

    private const val PREFS = "sim_scope"
    private const val KEY = "scope_label"

    // Lựa chọn THÔ đang lưu (null = tất cả). Đọc trong @Composable → tự recompose khi đổi.
    private var scopeState by mutableStateOf<String?>(null)

    /** Lựa chọn thô (để tô ô đang chọn ở Cài đặt). */
    val scope: String? get() = scopeState

    /**
     * Nhãn scope CÓ HIỆU LỰC: chỉ giữ khi SIM đó ĐANG LẮP ([SimRegistry.labels]). SIM đã tháo → trả `null` =
     * tất cả, tránh kẹt lọc ngầm khiến CẢ app rỗng mà thẻ Cài đặt đã ẩn. Lựa chọn lưu VẪN giữ, tự sống lại nếu
     * lắp lại SIM đó.
     */
    val effectiveLabel: String? get() = scopeState?.takeIf { it in SimRegistry.labels }

    fun init(context: Context) {
        scopeState = prefs(context).getString(KEY, null)
    }

    fun set(context: Context, label: String?) {
        scopeState = label
        prefs(context).edit().putString(KEY, label).apply()
    }

    /** Áp phạm vi SIM TẠI GỐC (repository) sau khi đã gán nhãn. `null`/không hợp lệ → không lọc. */
    fun filter(entries: List<CallEntry>): List<CallEntry> {
        val label = effectiveLabel ?: return entries
        return entries.filter { it.simLabel == label }
    }

    /**
     * Các account-id của SIM đang khoá (để lọc NGAY trong truy vấn SQL: `PHONE_ACCOUNT_ID IN (…)`). `null` = xem
     * tất cả / phạm vi không hợp lệ. Nơi gọi (loadRecent) phải [SimRegistry.refresh] trước khi hỏi.
     */
    fun scopeAccountIds(): List<String>? =
        effectiveLabel?.let { SimRegistry.accountIdsForLabel(it).takeIf { ids -> ids.isNotEmpty() } }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
