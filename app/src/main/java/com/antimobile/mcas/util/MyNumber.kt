package com.antimobile.mcas.util

import android.content.Context
import android.content.SharedPreferences
import com.antimobile.mcas.data.backup.MergeMode
import com.antimobile.mcas.data.backup.MyNumberEntry
import com.antimobile.mcas.data.backup.SectionResult

/**
 * Lưu SỐ ĐIỆN THOẠI của chính người dùng theo TỪNG KHE SIM, do người dùng TỰ NHẬP trong Cài đặt.
 *
 * Vì sao cần tự nhập: Android (và đa số nhà mạng VN) KHÔNG cung cấp số thuê bao (MSISDN) từ SIM —
 * `SubscriptionInfo.number`/`SubscriptionManager.getPhoneNumber()` thường rỗng. Khi KHÔNG đọc được số
 * tự động, người dùng nhập tay MỘT LẦN; số này được dùng cho pattern {phonesim1}/{phonesim2} trong mẫu
 * tin nhắn và cho "QR của tôi". Nếu máy/nhà mạng CÓ cho đọc tự động thì số tự đọc được ưu tiên (xem
 * [SimInfo.readDeviceSims]), lúc đó KHÔNG cần nhập tay.
 *
 * Lưu ở SharedPreferences JSON đơn giản, khoá theo khe (slotIndex 0 = "sim1", 1 = "sim2"). Số lưu ở
 * DẠNG THUẦN CHỮ SỐ (đã lọc ký tự thừa); rỗng = chưa nhập.
 */
object MyNumberStore {

    private const val PREFS = "my_numbers"

    private fun key(slotIndex: Int) = "sim${slotIndex + 1}"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Số người dùng đã nhập cho khe [slotIndex] (0 = SIM 1, 1 = SIM 2); rỗng nếu chưa nhập. */
    fun get(context: Context, slotIndex: Int): String =
        prefs(context).getString(key(slotIndex), "").orEmpty()

    /**
     * Ghi số cho khe [slotIndex] — CHỈ giữ chữ số (bỏ khoảng trắng, '+', '-'…). Chuỗi rỗng = xoá số đã lưu.
     * Việc kiểm tra hợp lệ (độ dài, đầu số) do lớp UI đảm nhiệm trước khi gọi.
     */
    fun set(context: Context, slotIndex: Int, number: String) {
        val digits = number.filter { it.isDigit() }
        prefs(context).edit().putString(key(slotIndex), digits).apply()
    }

    /** Số máy hỗ trợ tối đa (khe 0 = SIM 1, khe 1 = SIM 2) — dùng cho sao lưu. */
    private const val MAX_SLOTS = 2

    /** Xuất mọi số đã nhập (bỏ khe rỗng) cho bản sao lưu. */
    fun exportAll(context: Context): List<MyNumberEntry> =
        (0 until MAX_SLOTS).mapNotNull { slot ->
            get(context, slot).takeIf { it.isNotBlank() }?.let { MyNumberEntry(slot, it) }
        }

    /**
     * KHÔI PHỤC số của tôi theo [mode]. Số là dữ liệu VÔ HƯỚNG theo khe:
     *  - Khe đang RỖNG → luôn điền số từ bản sao lưu.
     *  - Khe đã có số + [MergeMode.ADD] → GIỮ số hiện có (bỏ qua).
     *  - Khe đã có số + [MergeMode.REPLACE]/[MergeMode.UPDATE] → GHI ĐÈ theo bản sao lưu.
     */
    fun restore(context: Context, entries: List<MyNumberEntry>, mode: MergeMode): SectionResult {
        var added = 0
        var updated = 0
        var skipped = 0
        for (e in entries) {
            if (e.slot !in 0 until MAX_SLOTS || e.number.isBlank()) {
                skipped++; continue
            }
            val current = get(context, e.slot)
            when {
                current.isBlank() -> {
                    set(context, e.slot, e.number); added++
                }
                mode == MergeMode.ADD -> skipped++
                else -> {
                    set(context, e.slot, e.number); updated++
                }
            }
        }
        return SectionResult(added = added, updated = updated, skipped = skipped)
    }
}
