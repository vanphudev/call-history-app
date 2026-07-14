package com.antimobile.callhs.util

import java.text.Normalizer

/**
 * Chuẩn hoá nội dung tin nhắn về dạng "không dấu, thuần ASCII" để mã hoá được bằng bảng GSM‑7 (160 ký tự/đoạn)
 * thay vì UCS‑2 (70 ký tự/đoạn) — giúp tin SMS TỐN ÍT PHÍ hơn.
 *
 * CHỈ dùng khi CHUYỂN nội dung sang ứng dụng nhắn tin (xem [CallActions.messageWithBody]/[CallActions.composeMessage]);
 * nội dung LƯU & HIỂN THỊ trong ứng dụng vẫn giữ nguyên Unicode + dấu.
 *
 * Quy tắc:
 *  - Bỏ dấu tiếng Việt: chuẩn hoá NFD rồi xoá dấu tổ hợp, quy Đ→D / đ→d (Đ không tự tách khi NFD).
 *  - Chuyển vài ký tự Unicode "in ấn" thường gặp về ASCII tương đương (nháy cong, gạch dài, ellipsis, bullet, ₫…).
 *  - Khoảng trắng Unicode (nbsp…) → khoảng trắng thường; ký tự rộng-0/BOM → bỏ.
 *  - GIỮ dấu câu ASCII thông thường (. , ! ? : ; …) vì chúng vốn thuộc GSM‑7.
 *  - Bỏ mọi ký tự Unicode còn lại KHÔNG phải ASCII (emoji, ✅, ký hiệu lạ…) để đảm bảo mã hoá GSM‑7.
 *
 * Dùng escape \uXXXX cho các nhánh nên NGUỒN KHÔNG chứa ký tự vô hình khó review.
 * Hàm THUẦN (không phụ thuộc Android) nên kiểm thử được bằng JVM unit test.
 */
object SmsText {

    private val COMBINING_MARKS = "\\p{M}+".toRegex()

    fun toGsm7(input: String): String {
        val noDiacritics = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace('Đ', 'D')   // Đ
            .replace('đ', 'd')   // đ

        val sb = StringBuilder(noDiacritics.length)
        for (ch in noDiacritics) {
            when (ch) {
                // Nháy đơn cong ‘ ’ ‚ / dấu sắc rời ′ / backtick ` / dấu sắc phẩy ´ → nháy đơn thẳng '
                '‘', '’', '‚', '′', '`', '´' -> sb.append('\'')
                // Nháy kép cong “ ” „ / dấu phẩy kép ″ → nháy kép thẳng "
                '“', '”', '„', '″' -> sb.append('"')
                // Gạch nối dài – — ― / dấu trừ − → gạch nối ASCII -
                '–', '—', '―', '−' -> sb.append('-')
                // Ellipsis … → "..."
                '…' -> sb.append("...")
                // Chấm đầu dòng • · ∙ ● ▪ → "*"
                '•', '·', '∙', '●', '▪' -> sb.append('*')
                '₫' -> sb.append('d')     // ₫ (đồng)
                '€' -> sb.append("EUR")   // €
                else -> when {
                    // Giữ ASCII in được + xuống dòng \n \r (đều thuộc GSM‑7). LƯU Ý: KHÔNG giữ TAB (\t) — tab
                    // KHÔNG có trong GSM‑7, giữ lại sẽ ép cả tin sang UCS‑2; nó rơi xuống nhánh whitespace → space.
                    ch.code in 0x20..0x7E || ch == '\n' || ch == '\r' -> sb.append(ch)
                    // Khoảng trắng khác (tab, nbsp, thin, narrow-nbsp…) → khoảng trắng thường.
                    ch.isWhitespace() -> sb.append(' ')
                    // Còn lại (emoji, ZWSP/BOM, ký hiệu lạ…) → BỎ.
                    else -> Unit
                }
            }
        }
        return sb.toString()
    }
}
