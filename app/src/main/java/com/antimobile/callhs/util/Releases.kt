package com.antimobile.callhs.util

/**
 * MỘT BẢN PHÁT HÀNH — khai đủ mọi thứ bản đó làm, ở MỘT chỗ.
 *
 * @param versionCode Khớp `versionCode` trong `app/build.gradle.kts`. Đây là KHOÁ của mọi cơ chế.
 * @param versionName Chỉ để đọc cho dễ; không ảnh hưởng hành vi.
 * @param summary Ghi chú lịch sử cho chính bạn về sau. Không hiển thị cho người dùng.
 * @param features Gạch đầu dòng cho modal "Có gì mới". RỖNG ⇒ không hiện modal.
 * @param policyMessage KHÁC `null` ⇒ bản này ĐỔI chính sách, hiện modal với đúng nội dung này.
 * @param resetAgencyCache `true` ⇒ xoá cache danh bạ cơ quan, người dùng vào là tải lại NGAY (bỏ qua TTL 7 ngày).
 * @param resetLegalCache `true` ⇒ xoá cache chính sách/điều khoản, tải lại NGAY (bỏ qua TTL 30 ngày).
 * @param newTemplates Mẫu tin nhắn hệ thống THÊM ở bản này. Chỉ được THÊM, không bao giờ đè/xoá mẫu người dùng.
 */
data class Release(
    val versionCode: Int,
    val versionName: String,
    val summary: String = "",
    val features: List<String> = emptyList(),
    val policyMessage: String? = null,
    val resetAgencyCache: Boolean = false,
    val resetLegalCache: Boolean = false,
    val newTemplates: List<SystemTemplate> = emptyList()
)

/**
 * ★★ NHẬT KÝ PHÁT HÀNH — NGUỒN SỰ THẬT DUY NHẤT ★★
 *
 * Đây là **file duy nhất** bạn sửa khi ra bản mới (ngoài `versionCode`/`versionName` trong
 * `app/build.gradle.kts`, thứ Google Play bắt buộc phải nằm ở đó).
 *
 * ### Ra bản mới: thêm MỘT entry
 * ```
 * Release(
 *     versionCode = 5,                       // = versionCode trong build.gradle.kts
 *     versionName = "26.2.1",
 *     summary     = "Đổi bảng giá cước; cập nhật danh bạ Hà Nội.",
 *     features    = listOf("Thống kê gọi lại theo tháng"),   // bỏ trống nếu không báo gì
 *     policyMessage = "Chúng tôi vừa cập nhật …",            // bỏ đi nếu chính sách không đổi
 *     resetAgencyCache = true,                               // buộc tải lại danh bạ ngay
 *     newTemplates = listOf(SystemTemplate("sys_v5_hen_giao_lai", "5. Hẹn giao lại", "…"))
 * )
 * ```
 * Bản nào **không có gì để khai thì không cần entry** — mọi cơ chế tự im lặng.
 *
 * ### Ba quy tắc bất di bất dịch
 * 1. **KHÔNG BAO GIỜ sửa/xoá một entry đã phát hành.** Nó là lịch sử; sửa = viết lại quá khứ và
 *    người dùng cũ sẽ nhận nhầm thông báo / mẫu.
 * 2. **KHÔNG BAO GIỜ đổi [SystemTemplate.stableKey]** đã ship — đổi là coi như mẫu mới, sẽ gieo lại
 *    cho người đã có (kể cả người đã cố ý xoá nó).
 * 3. Entry xếp theo `versionCode` TĂNG DẦN. Được phép nhảy số (3 → 5 → 6).
 *
 * ### Ai đọc file này
 * - [AppMigrations] — chạy `resetAgencyCache` / `resetLegalCache` / `newTemplates` đúng một lần.
 * - [WhatsNewStore] — quyết định hiện modal `features` / `policyMessage`.
 * - [MessageTemplateStore] — gieo trọn bộ mẫu cho máy cài mới.
 *
 * Người dùng **nhảy cóc** nhiều bản (đang ở 3, cập nhật thẳng lên 6) vẫn được áp dụng đủ mọi entry
 * trong khoảng, theo đúng thứ tự — không sót reset nào, không sót mẫu nào, thông báo được gộp lại.
 */
object Releases {

    /**
     * `versionCode` của bản ĐẦU TIÊN có hệ thống cập nhật này.
     *
     * Người dùng cài từ bản CŨ HƠN (app chưa có [AppMigrations]) được coi như đang đứng ở đây: mọi thứ
     * khai ở các entry `<=` số này họ đã có sẵn rồi, chỉ áp dụng những entry MỚI HƠN.
     */
    const val BASELINE_VERSION_CODE = 1

    val ALL: List<Release> = listOf(

        Release(
            versionCode = 1,
            versionName = "26.1.1",
            summary = "Mốc gốc. Bốn mẫu tin nhắn đầu tiên. Bản đầu tiên có hệ thống cập nhật/reset.",
            newTemplates = listOf(
                SystemTemplate(
                    stableKey = "sys_v1_copy_gui_lai",
                    title = "1. Yêu cầu khách copy gửi lại",
                    content = """
                    Anh/chi vui long copy tin nhan ben duoi va gui lai theo dung thu tu de xac nhan.
                """.trimIndent()
                ),
                SystemTemplate(
                    stableKey = "sys_v1_di_vang_nho_shipper",
                    title = "2. Khách đi vắng nhờ Shipper gửi hàng",
                    content = """
                    1. Toi di vang, nho Shipper gui hang tai [ ] 
                    • Ma don: {contextqr}. 
                    • Luc: {datetime}.
                """.trimIndent()
                ),
                SystemTemplate(
                    stableKey = "sys_v1_xac_nhan_da_nhan",
                    title = "3. Xác nhận đã nhận hàng",
                    content = """
                    2. Toi da nhan don hang Shopee hoa toc voi • Ma Don: {contextqr}, vao luc • {datetime}, hang con nguyen ven.
                """.trimIndent()
                ),
                SystemTemplate(
                    stableKey = "sys_v1_chuyen_khoan",
                    title = "4. Chuyển khoản - Ngân hàng",
                    content = """
                    Bank:
                    STK:
                    Tong:
                """.trimIndent()
                )
            )
        )

        // ── BẢN TIẾP THEO THÊM VÀO ĐÂY (versionCode tăng dần). Ví dụ đầy đủ: ──
        // ,
        // Release(
        //     versionCode = 5,
        //     versionName = "26.2.1",
        //     summary = "Cập nhật danh bạ Hà Nội; sửa Chính sách quyền riêng tư; thêm mẫu hẹn giao lại.",
        //     features = listOf(
        //         "Thống kê gọi lại theo tháng",
        //         "Tìm kiếm danh bạ nhanh hơn"
        //     ),
        //     policyMessage = "Chúng tôi vừa cập nhật Chính sách quyền riêng tư: …",
        //     resetAgencyCache = true,
        //     newTemplates = listOf(
        //         SystemTemplate("sys_v5_hen_giao_lai", "5. Hẹn giao lại", "Toi dang ban, nho Shipper giao lai vao luc [ ].")
        //     )
        // )
    )

    /** Luôn xét theo thứ tự phiên bản, không phụ thuộc thứ tự gõ trong [ALL]. */
    private val sorted: List<Release> = ALL.sortedBy { it.versionCode }

    /** Bản mới nhất từng khai. */
    val LATEST: Release get() = sorted.last()

    /**
     * Các bản trong khoảng `(seenVersion, currentVersion]`, theo thứ tự tăng dần.
     *
     * Đây là hạt nhân của MỌI cơ chế: "những gì đã xảy ra kể từ lúc máy này biết tới, cho tới bản đang
     * chạy". Người nhảy cóc phiên bản tự động nhận đủ; entry của bản chưa phát hành bị bỏ qua.
     */
    fun since(seenVersion: Int, currentVersion: Int): List<Release> =
        sorted.filter { it.versionCode > seenVersion && it.versionCode <= currentVersion }

    /** Toàn bộ mẫu hệ thống từng phát hành, theo đúng thứ tự — dùng gieo cho máy CÀI MỚI. */
    fun allTemplates(): List<SystemTemplate> = sorted.flatMap { it.newTemplates }

    /** Mẫu hệ thống của các bản `<= versionCode` — dùng đánh dấu "đã chào" cho người dùng cũ. */
    fun templatesUpTo(versionCode: Int): List<SystemTemplate> =
        sorted.filter { it.versionCode <= versionCode }.flatMap { it.newTemplates }
}
