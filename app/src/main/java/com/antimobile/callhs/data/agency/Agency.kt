package com.antimobile.callhs.data.agency

import java.text.Normalizer

/**
 * Một CƠ QUAN (điểm/trung tâm phục vụ hành chính công, hoặc công an phường/xã) trong danh bạ.
 * Một cơ quan có thể có NHIỀU số điện thoại. Khi CHƯA có số ([phones] rỗng) mà là công an thì
 * [fallbackPhone] thường là "113" (số khẩn cấp).
 *
 * [searchText] = "tên + phường/xã + địa chỉ" đã bỏ dấu + viết thường, TÍNH SẴN lúc phân tích để tìm
 * kiếm full-text không phân biệt hoa/thường & dấu mà không phải chuẩn hoá lại mỗi lần gõ.
 */
data class Agency(
    val id: String,
    val name: String,
    val ward: String,
    val address: String,
    val phones: List<String>,
    val fallbackPhone: String?,
    val maps: String?,
    val facebook: String?,
    val searchText: String
) {
    /** Số điện thoại chính (đầu tiên) — dùng cho nút gọi ở dòng thu gọn. */
    val primaryPhone: String? get() = phones.firstOrNull()
    val hasPhone: Boolean get() = phones.isNotEmpty()
}

/** Số điện thoại khẩn cấp chung của một tỉnh/ngành (đọc từ thẻ `<emergency>` trong file .xml). */
data class Emergency(
    val police: String?,
    val fire: String?,
    val medical: String?,
    val note: String?
)

/**
 * Toàn bộ dữ liệu 1 tập (tỉnh × ngành) sau khi đọc file .xml: metadata + danh sách cơ quan.
 */
data class AgencyData(
    val dataset: AgencyDataset,
    val provinceName: String,
    val category: String,
    val updated: String?,
    val source: String?,
    val disclaimer: String?,
    val emergency: Emergency?,
    val agencies: List<Agency>
)

/** Tỉnh / thành phố. */
enum class AgencyProvince(val key: String, val displayName: String, val shortName: String) {
    HANOI("hanoi", "Hà Nội", "Hà Nội"),
    HCM("hcm", "TP. Hồ Chí Minh", "TP.HCM");

    companion object {
        fun fromKey(key: String?): AgencyProvince? = entries.firstOrNull { it.key == key }
    }
}

/**
 * NGÀNH cơ quan. [key] = phần đuôi tên file (`hanhchinh`/`congan`); [xmlValue] = giá trị thuộc tính
 * `branch` trong file .xml (`admin`/`police`).
 */
enum class AgencyBranch(
    val key: String,
    val xmlValue: String,
    val displayName: String,
    val shortName: String
) {
    ADMIN("hanhchinh", "admin", "Cơ quan hành chính công", "Hành chính công"),
    POLICE("congan", "police", "Cơ quan công an", "Công an");

    companion object {
        fun fromKey(key: String?): AgencyBranch? = entries.firstOrNull { it.key == key }
    }
}

/**
 * MỘT TẬP DỮ LIỆU = 1 tỉnh × 1 ngành → 1 file `.xml` trên GitHub (`call-history-site/data/<key>.xml`).
 *
 * - [key] = "`<tỉnh>_<ngành>`" (vd `hcm_congan`) = tên file (bỏ `.xml`), cũng là khoá cache.
 *
 * PURE REMOTE: dữ liệu CHỈ lấy từ GitHub rồi lưu cache tại máy — KHÔNG còn bản seed đóng gói trong app.
 * Thêm tỉnh/ngành mới: (1) thả file `<key>.xml` vào `call-history-site/data/`, (2) thêm 1 dòng ở đây.
 */
enum class AgencyDataset(
    val province: AgencyProvince,
    val branch: AgencyBranch
) {
    HANOI_ADMIN(AgencyProvince.HANOI, AgencyBranch.ADMIN),
    HANOI_POLICE(AgencyProvince.HANOI, AgencyBranch.POLICE),
    HCM_ADMIN(AgencyProvince.HCM, AgencyBranch.ADMIN),
    HCM_POLICE(AgencyProvince.HCM, AgencyBranch.POLICE);

    val key: String get() = "${province.key}_${branch.key}"
    val fileName: String get() = "$key.xml"
    val title: String get() = province.displayName
    val category: String get() = branch.displayName

    companion object {
        fun fromKey(key: String?): AgencyDataset? = entries.firstOrNull { it.key == key }
        fun of(province: AgencyProvince, branch: AgencyBranch): AgencyDataset =
            entries.first { it.province == province && it.branch == branch }
    }
}

/**
 * Chuẩn hoá chuỗi tiếng Việt để TÌM KIẾM: viết thường, tách dấu (NFD) rồi BỎ dấu thanh/dấu phụ,
 * đổi đ→d. Nhờ vậy gõ "tan dinh" tìm được "Tân Định", "cong an sai gon" tìm "Công an Phường Sài Gòn".
 *
 * KHÔNG dùng regex `\p{Mn}` (một số máy Android/ICU nhạy cảm với cú pháp `{}` trong regex) —
 * lọc dấu phụ bằng [Character.getType] cho an toàn.
 */
fun normalizeForSearch(input: String): String {
    if (input.isEmpty()) return ""
    val nfd = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
    val sb = StringBuilder(nfd.length)
    for (c in nfd) {
        when {
            Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> {} // bỏ dấu phụ
            c == 'đ' -> sb.append('d')
            else -> sb.append(c)
        }
    }
    return sb.toString().trim().replace(Regex("\\s+"), " ")
}
