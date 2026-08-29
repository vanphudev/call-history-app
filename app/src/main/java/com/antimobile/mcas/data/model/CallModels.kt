package com.antimobile.mcas.data.model

import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.util.DeviceInfo
import com.antimobile.mcas.util.SimRegistry
import com.antimobile.mcas.util.SpecialNumbers

/** Loại cuộc gọi theo CallLog.Calls.TYPE. */
enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL, ANSWERED_EXTERNALLY, UNKNOWN
}

/** Một bản ghi cuộc gọi đọc trực tiếp từ máy (chỉ đọc). */
data class CallEntry(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val type: CallType,
    val dateMillis: Long,
    val durationSeconds: Long,
    val photoUri: String?,
    val geocodedLocation: String?,
    val numberType: Int,           // CACHED_NUMBER_TYPE (Phone.TYPE_*) — nhãn hiển thị resolve ở UI theo ngôn ngữ
    val numberTypeCustom: String?, // nhãn tự đặt của liên hệ khi type = TYPE_CUSTOM (dữ liệu người dùng, giữ nguyên)
    val phoneAccountId: String?,   // id tài khoản điện thoại (dùng suy ra SIM)
    val simLabel: String?,         // "SIM 1" / "SIM 2" (suy ra từ phoneAccountId)
    val carrier: String?,          // nhà mạng suy từ đầu số (Viettel/MobiFone/…)
    val isVideo: Boolean,          // cuộc gọi video (CallLog.Calls.FEATURES_VIDEO)
    val isVolte: Boolean           // cuộc gọi VoLTE (CallLog.Calls.FEATURES_VOLTE)
) {
    // Ưu tiên tên DANH BẠ (cachedName); chưa lưu thì dùng TÊN KHẨN CẤP cho 113/114/115; cuối cùng là số.
    val nameOrNumber: String
        get() = cachedName?.takeIf { it.isNotBlank() }
            ?: SpecialNumbers.name(number)
            ?: number.ifBlank { appStrings().common.hiddenNumber }

    /** Khu vực: vị trí geocode từ đầu số, hoặc region của thiết bị (mặc định "Việt Nam"). */
    val region: String
        get() = geocodedLocation?.takeIf { it.isNotBlank() } ?: DeviceInfo.regionName

    /**
     * Nhãn SIM để HIỂN THỊ trên UI: chỉ trả về khi máy đang lắp ≥2 SIM ([SimRegistry.multiSim]); máy 1 SIM →
     * `null` (không hiện tag "SIM 1" vì thừa & dễ gây hiểu nhầm). KHÁC với [simLabel] thô (vẫn dùng cho lọc/
     * phạm vi SIM). Mọi nơi HIỂN THỊ nhãn SIM nên dùng thuộc tính này để chỉ cần sửa một chỗ.
     */
    val displaySimLabel: String?
        get() = simLabel?.takeIf { SimRegistry.multiSim }
}

/** Một dòng trong danh sách: gộp các cuộc gọi liên tiếp cùng số (giống Google Phone). */
data class CallListItem(
    val number: String,
    val cachedName: String?,
    val photoUri: String?,
    val latest: CallEntry,
    val count: Int
) {
    val nameOrNumber: String
        get() = cachedName?.takeIf { it.isNotBlank() }
            ?: SpecialNumbers.name(number)
            ?: number.ifBlank { appStrings().common.hiddenNumber }
}

/** Tổng hợp toàn bộ lịch sử với một số, dùng cho màn chi tiết. */
data class CallNumberDetail(
    val number: String,
    val cachedName: String?,
    val photoUri: String?,
    val geocodedLocation: String?,
    val carrier: String?,
    val simLabels: List<String>,   // các SIM đã dùng với số này, ví dụ ["SIM 1","SIM 2"]
    val entries: List<CallEntry>,
    val totalCalls: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val totalDurationSeconds: Long,
    /** URI liên hệ (lookup) nếu số này ĐANG là liên hệ đã lưu — dùng để mở XEM trong Danh bạ. */
    val contactLookupUri: String? = null
) {
    val nameOrNumber: String
        get() = cachedName?.takeIf { it.isNotBlank() }
            ?: SpecialNumbers.name(number)
            ?: number.ifBlank { appStrings().common.hiddenNumber }

    /** Số này có đang được lưu trong Danh bạ không (theo tra cứu thời gian thực bằng PhoneLookup). */
    val isSavedContact: Boolean
        get() = contactLookupUri != null

    /** Tên hiển thị cố định: tên danh bạ → TÊN KHẨN CẤP (113/114/115) → "Chưa lưu danh bạ" nếu số lạ. */
    val displayName: String
        get() = cachedName?.takeIf { it.isNotBlank() }
            ?: SpecialNumbers.name(number)
            ?: appStrings().callDetail.notSavedContact

    /** Khu vực: vị trí geocode từ đầu số, hoặc region của thiết bị (mặc định "Việt Nam"). */
    val region: String
        get() = geocodedLocation?.takeIf { it.isNotBlank() } ?: DeviceInfo.regionName

    /**
     * Các nhãn SIM để HIỂN THỊ (huy hiệu ở đầu màn chi tiết): rỗng khi máy chỉ 1 SIM ([SimRegistry.multiSim] =
     * false) để không hiện huy hiệu "SIM 1" thừa. [simLabels] thô vẫn dùng cho thanh lọc/số liệu theo SIM.
     */
    val displaySimLabels: List<String>
        get() = if (SimRegistry.multiSim) simLabels else emptyList()

    val lastCallMillis: Long?
        get() = entries.firstOrNull()?.dateMillis
}
