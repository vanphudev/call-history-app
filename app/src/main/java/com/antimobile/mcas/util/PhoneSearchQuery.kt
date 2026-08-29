package com.antimobile.mcas.util

/**
 * Dựng CÂU TRUY VẤN tìm kiếm SÂU cho một số điện thoại trên web (Google).
 *
 * Ý tưởng: cùng một số có thể được viết trên mạng theo NHIỀU ĐỊNH DẠNG khác nhau
 * (nội địa `0987654321`, có nhóm `0987 654 321`, quốc tế `+84987654321`, `+84 987 654 321`,
 * hay `84987654321`). Để bắt được MỌI trang có nhắc tới số này, ta gom tất cả biến thể lại
 * và nối bằng toán tử `OR` của Google, mỗi biến thể đặt trong dấu ngoặc kép để KHỚP CHÍNH XÁC.
 *
 * Đây là hàm THUẦN (không phụ thuộc Android) nên kiểm thử được bằng JVM unit test.
 */
object PhoneSearchQuery {

    /**
     * Sinh danh sách các BIẾN THỂ định dạng (đã khử trùng lặp, giữ thứ tự) của [rawPhone].
     * Với số VN chuẩn (bắt đầu `0` hoặc `84`) sẽ có đủ dạng nội địa + quốc tế; số khác chỉ
     * giữ dạng gốc + dạng có nhóm để không sinh biến thể rác.
     */
    fun variants(rawPhone: String): List<String> {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isEmpty()) return emptyList()

        val set = LinkedHashSet<String>()
        if (digits.startsWith("84") || digits.startsWith("0")) {
            // Số VN chuẩn → nội địa (0…) + quốc tế (84…, +84…), mỗi dạng kèm bản CÓ NHÓM.
            // Dạng có nhóm đi qua SỐ NỘI ĐỊA để [formatPhone] nhóm đúng (tránh nhóm rác trên 84…).
            val intl = PhoneNormalizer.toIntl(rawPhone)                       // 84987654321
            val national = if (digits.startsWith("84")) "0" + digits.drop(2) else digits
            set.add(national)                                                 // 0987654321
            formatPhone(national).let { if (it.isNotBlank()) set.add(it) }    // 0987 654 321
            if (intl.isNotBlank()) {
                set.add("+$intl")                                            // +84987654321
                intlPlusSpaced(national)?.let { set.add(it) }                // +84 987 654 321
                set.add(intl)                                                // 84987654321
            }
        } else {
            // Số khác (hotline 1900/1800, số ngoài VN…) → chỉ giữ dạng gốc + dạng có nhóm,
            // KHÔNG ghép tiền tố 84/0 để tránh sinh biến thể rác.
            set.add(digits)
            formatPhone(digits).let { if (it.isNotBlank()) set.add(it) }
        }
        return set.filter { it.isNotBlank() }.toList()
    }

    /**
     * Câu truy vấn Google tìm SÂU: nối các biến thể bằng ` OR `, mỗi biến thể trong ngoặc kép.
     * Trả về **chuỗi rỗng** với số ẩn/không xác định (rỗng) hoặc marker trình bày của Android
     * (`-1` hạn chế, `-2` không rõ, `-3` payphone) — nơi gọi dựa vào chuỗi rỗng để báo lỗi thay vì
     * tìm kiếm số rác.
     */
    fun buildGoogleQuery(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("-")) return ""
        val variants = variants(trimmed)
        if (variants.isEmpty()) return ""
        return variants.joinToString(" OR ") { "\"$it\"" }
    }

    /** national kiểu "0987654321" → "+84 987 654 321" (dựa trên cách nhóm của [formatPhone]). */
    private fun intlPlusSpaced(national: String): String? {
        val spaced = formatPhone(national)
        if (!spaced.startsWith("0")) return null
        return "+84 " + spaced.drop(1).trim()
    }
}
