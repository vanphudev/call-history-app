package com.antimobile.callhs.util

/**
 * KHOÁ SO KHỚP số điện thoại — nguồn DUY NHẤT toàn app để nhận biết "hai chuỗi là cùng một số".
 *
 * Trả về SỐ THUÊ BAO NỘI ĐỊA (national significant number): bỏ ký tự thừa, bỏ mã truy cập quốc tế `00`,
 * bỏ mã quốc gia `84` (dạng `+84…`/`84…`) và bỏ số `0` trung kế đầu (dạng nội địa `0…`). Nhờ đó
 * `+84 912 345 678`, `0912345678`, `912345678` đều ra cùng khoá `912345678`.
 *
 * ĐÃ SỬA so với bản "9 số cuối": bản cũ cắt cứng 9 số cuối nên số CỐ ĐỊNH 11 số (vd `028 3822 1234`
 * → `838221234`) bị VA CHẠM với di động 10 số (`083 822 1234` → `838221234`). Bản này giữ NGUYÊN độ dài
 * NSN (di động 9 số, cố định 9–10 số) nên hai loại đó ra khoá khác nhau. Với DI ĐỘNG (đại đa số dữ liệu),
 * kết quả GIỐNG HỆT bản cũ nên dữ liệu nhóm phân loại đã lưu (khoá theo bản cũ) vẫn khớp; riêng thành viên
 * là số cố định được chuẩn hoá lại 1 lần lúc khởi động (xem CategoryRepository.renormalizeMemberKeys).
 *
 * Số ngắn/đặc biệt/khẩn cấp (dưới 9 số: 113, 1900xxxx…) và số QUỐC TẾ không phải VN được GIỮ NGUYÊN
 * chuỗi chữ số để không gộp nhầm.
 */
object PhoneKey {

    /** Chỉ giữ chữ số của [raw]. */
    fun digits(raw: String): String = raw.filter(Char::isDigit)

    /** Khoá so khớp (NSN nội địa) của [raw]; rỗng nếu [raw] không có chữ số nào. */
    fun of(raw: String): String {
        var d = raw.filter(Char::isDigit)
        if (d.isEmpty()) return ""
        // Bỏ mã truy cập quốc tế "00" (vd 0084912345678 → 84912345678).
        if (d.length > 2 && d.startsWith("00")) d = d.substring(2)
        // Bỏ mã quốc gia VN "84" khi số ở dạng quốc tế (84 + ≥9 số NSN). Không đụng số 8 chữ số bắt đầu bằng 84.
        if (d.startsWith("84") && d.length >= 11) d = d.substring(2)
        // Bỏ số 0 trung kế đầu ở dạng nội địa (0 + NSN, tối thiểu 10 số như 0xxxxxxxxx / 0xx xxxxxxx).
        if (d.startsWith("0") && d.length >= 10) d = d.substring(1)
        return d
    }

    /** [a] và [b] có phải CÙNG một số điện thoại không (chấp nhận 0 ↔ +84 ↔ 00 ↔ khoảng trắng/dấu). */
    fun sameNumber(a: String, b: String): Boolean {
        val ka = of(a)
        return ka.isNotEmpty() && ka == of(b)
    }

    /** [query] có "trông giống" một số điện thoại không (chỉ chữ số + khoảng trắng/`+`/`-`/`.`/`()`, có ≥1 số). */
    fun isPhoneLikeQuery(query: String): Boolean {
        val q = query.trim()
        return q.isNotEmpty() && q.any(Char::isDigit) && q.all { it.isDigit() || it in " +-.()" }
    }

    /**
     * Số [rawNumber] có KHỚP truy vấn số [query] không: khớp chuỗi con chữ số (gõ một đoạn giữa số) HOẶC
     * khớp theo khoá chuẩn để `0…` và `+84…` xem như nhau. Gọi kèm [isPhoneLikeQuery] để không khớp nhầm
     * khi người dùng gõ chữ lẫn số (vd tên "Lan 84").
     */
    fun matchesQuery(rawNumber: String, query: String): Boolean {
        val qDigits = query.filter(Char::isDigit)
        if (qDigits.isEmpty()) return false
        val nDigits = rawNumber.filter(Char::isDigit)
        if (nDigits.isEmpty()) return false
        if (nDigits.contains(qDigits)) return true
        val qKey = of(query)
        val nKey = of(rawNumber)
        return qKey.isNotEmpty() && nKey.contains(qKey)
    }
}
