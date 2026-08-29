package com.antimobile.mcas.util

import androidx.annotation.DrawableRes
import com.antimobile.mcas.R
import com.antimobile.mcas.i18n.EmergencyKind
import com.antimobile.mcas.i18n.appStrings

/**
 * Các SỐ KHẨN CẤP đặc biệt của Việt Nam — hiển thị TÊN + ICON mặc định (huy hiệu ngành) khi số CHƯA được
 * lưu trong Danh bạ. Nếu số ĐÃ lưu danh bạ thì luôn ƯU TIÊN tên/ảnh danh bạ: nơi gọi kiểm tra `cachedName`
 * (danh tính PhoneLookup sống) trước → còn null mới rơi xuống [of]/[name] ở đây.
 *
 * Ánh xạ số → ảnh dùng `res/drawable/ic_113|114|115.png` (đã sửa cho khớp đúng thực tế):
 * 113 = Cảnh sát (Công an), 114 = Cứu hoả (PCCC & CNCH), 115 = Cấp cứu (y tế).
 */
object SpecialNumbers {

    /** LOẠI khẩn cấp + tài nguyên icon. TÊN tách riêng (đổi theo ngôn ngữ) — xem [name]. */
    data class Info(val kind: EmergencyKind, @param:DrawableRes val iconRes: Int)

    private val byNumber: Map<String, Info> = mapOf(
        "113" to Info(EmergencyKind.POLICE, R.drawable.ic_113),
        "114" to Info(EmergencyKind.FIRE, R.drawable.ic_114),
        "115" to Info(EmergencyKind.MEDICAL, R.drawable.ic_115)
    )

    /**
     * Thông tin số khẩn cấp cho [number] (bỏ khoảng trắng/dấu ngăn cách rồi khớp CHÍNH XÁC "113/114/115");
     * `null` nếu không phải số đặc biệt. Số thường có chứa 113/114 (vd 0113…) sẽ KHÔNG khớp vì so khớp cả chuỗi.
     */
    fun of(number: String?): Info? {
        if (number == null) return null
        val digits = number.filterNot { it.isWhitespace() || it == '-' || it == '.' || it == '(' || it == ')' }
        return byNumber[digits]
    }

    /** Tên mặc định (ĐA NGÔN NGỮ) cho số khẩn cấp; `null` nếu không phải số đặc biệt. */
    fun name(number: String?): String? = of(number)?.let { appStrings().emergency.of(it.kind) }
}
