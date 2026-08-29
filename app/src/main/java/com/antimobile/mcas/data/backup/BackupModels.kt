package com.antimobile.mcas.data.backup

import com.antimobile.mcas.util.MessageTemplate
import com.antimobile.mcas.util.QrScanEntry

/**
 * MÔ HÌNH cho tính năng SAO LƯU & KHÔI PHỤC dữ liệu do app tự quản.
 *
 * Chỉ gồm dữ liệu app SỞ HỮU (mẫu tin nhắn, lịch sử quét QR, nhóm phân loại, bộ chặn, số của tôi,
 * cài đặt cuộc gọi đi và cài đặt hiển thị).
 * **Lịch sử cuộc gọi KHÔNG nằm trong đây** — đó là dữ liệu HỆ THỐNG chỉ-đọc ([CallLog]); app không thể ghi
 * lại vào máy (Android chỉ cho trình gọi mặc định làm việc đó) nên cố tình loại khỏi sao lưu.
 */

/** Một mảng dữ liệu có thể sao lưu/khôi phục độc lập. [jsonKey] là khoá trong file .json (bất biến). */
enum class BackupSection(val jsonKey: String) {
    TEMPLATES("templates"),
    QR_HISTORY("qrHistory"),
    CATEGORIES("categories"),
    BLOCK_RULES("callBlockRules"),
    BLOCK_HISTORY("blockedCalls"),
    MY_NUMBER("myNumbers"),
    OUTGOING_CALL("outgoingCallSettings"),
    DISPLAY("display");

    companion object {
        fun fromKey(key: String): BackupSection? = entries.firstOrNull { it.jsonKey == key }
    }
}

/**
 * Chế độ KHÔI PHỤC (áp cho MỌI mục được chọn trong một lần khôi phục):
 *  - [REPLACE] — GHI ĐÈ TOÀN BỘ: mục trở thành đúng nội dung trong bản sao lưu (xoá cái đang có).
 *  - [ADD]     — THÊM, KHÔNG GHI ĐÈ: chỉ thêm mục CHƯA có; đụng trùng thì GIỮ dữ liệu hiện tại.
 *  - [UPDATE]  — CẬP NHẬT & THÊM: thêm mục mới; đụng trùng thì LẤY theo bản sao lưu (QR: giữ mốc MỚI hơn).
 *
 * Với mục dạng VÔ HƯỚNG (số của tôi / cài đặt hiển thị): [ADD] = giữ giá trị hiện có (chỉ điền chỗ trống);
 * [REPLACE]/[UPDATE] = ghi đè theo bản sao lưu.
 */
enum class MergeMode { REPLACE, ADD, UPDATE }

/** Kết quả khôi phục MỘT mục. [truncated] = có phần bị bỏ vì chạm giới hạn (số nhóm/thành viên tối đa…). */
data class SectionResult(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val truncated: Boolean = false,
) {
    val total: Int get() = added + updated + skipped
}

/** Tổng hợp kết quả khôi phục theo từng mục — dùng cho hộp thoại tóm tắt. */
data class ImportReport(val sections: Map<BackupSection, SectionResult>) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

/** Một nhóm phân loại trong bản sao lưu (tách khỏi entity Room). [members] = các số thuộc nhóm. */
data class BackupCategory(
    val name: String,
    val description: String,
    val iconKey: String,
    val colorArgb: Long,
    val builtInKey: String?,   // "work"/"favorite" (nhóm mặc định) hoặc null (nhóm người dùng)
    val sortOrder: Int,
    val createdAt: Long,
    val members: List<BackupMember>,
)

/** Một số điện thoại thành viên trong bản sao lưu. */
data class BackupMember(
    val rawNumber: String,
    val phoneKey: String,
    val addedAt: Long,
)

/**
 * Cấu hình bộ chặn trong backup: quy tắc + công tắc bảo vệ + nhịp notification + policy bền vững.
 * Các trường cài đặt nullable để parser vẫn đọc được file phiên bản cũ/thiếu một trường.
 * Mốc tạm ngưng và ledger các lượt gọi đang đếm là runtime state, không thuộc backup.
 */
data class BackupBlockConfig(
    val enabled: Boolean?,
    val notificationMode: String?,
    /**
     * Storage key của phương thức xử lý cuộc gọi. Nullable để backup v1/v2 (chưa có trường này)
     * vẫn được khôi phục với hành vi hiện tại của ứng dụng.
     */
    val blockMethod: String?,
    val rules: List<BackupBlockRule>,
    /** Exact allow/block entries. Picker provenance is metadata, not a matching condition. */
    val numberEntries: List<BackupNumberEntry> = emptyList(),
    /** Cho phép số đã lưu trong danh bạ vượt qua các rule bao quát; direct rules vẫn ưu tiên. */
    val allowSavedContactsEnabled: Boolean? = null,
    /** Cổng bảo vệ số ngoài danh bạ không khớp rule; field mới không kế thừa semantics bypass cũ. */
    val repeatUnknownCallerGuardEnabled: Boolean? = null,
    val repeatUnknownCallerGuardThreshold: Int? = null,
    val repeatUnknownCallerGuardWindowMinutes: Int? = null,
    /** Null means an older backup omitted schedules; an empty list explicitly clears them. */
    val dailySchedule: List<BackupBlockScheduleWindow>? = null,
    /** Cấu hình âm thanh/rung/cách hiển thị thông báo chặn; null với backup v1-v5. */
    val advancedNotification: BackupBlockNotificationConfig? = null,
) {
    val hasSettings: Boolean
        get() = enabled != null ||
            notificationMode != null ||
            blockMethod != null ||
            allowSavedContactsEnabled != null ||
            repeatUnknownCallerGuardEnabled != null ||
            repeatUnknownCallerGuardThreshold != null ||
            repeatUnknownCallerGuardWindowMinutes != null ||
            dailySchedule != null ||
            advancedNotification != null

    val hasAny: Boolean
        get() = hasSettings || rules.isNotEmpty() || numberEntries.isNotEmpty()
}

/** Portable recurring local-clock window. Runtime one-shot pause deadlines remain excluded. */
data class BackupBlockScheduleWindow(
    val id: String,
    val action: String,
    val startMinute: Int,
    val endMinute: Int,
    val presetKey: String? = null,
    val enabled: Boolean = true,
    val weekdaysMask: Int = com.antimobile.mcas.data.blocking.ALL_WEEKDAYS_MASK,
)

/** Các lựa chọn thông báo có thể chuyển an toàn sang máy khác. */
data class BackupBlockNotificationAlert(
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    /** Chỉ lưu key của âm thanh đóng gói; URI tệp SAF tùy chỉnh không có tính di động. */
    val soundPreset: String,
    val presentation: String,
)

data class BackupBlockNotificationPeriod(
    val period: String,
    val enabled: Boolean,
    val alert: BackupBlockNotificationAlert,
)

data class BackupBlockNotificationConfig(
    val scheduleEnabled: Boolean,
    val defaultAlert: BackupBlockNotificationAlert,
    val periods: List<BackupBlockNotificationPeriod>,
)

/** Portable exact-number entry used by both the allowlist and blocklist. */
data class BackupNumberEntry(
    val action: String,
    val rawNumber: String,
    val phoneKey: String,
    val displayName: String,
    val origin: String,
    val enabled: Boolean,
    val createdAt: Long,
)

/** Một quy tắc chặn tách khỏi id Room để khôi phục vào máy khác không bị lệ thuộc id cũ. */
data class BackupBlockRule(
    val type: String,
    val rawValue: String,
    val matchValue: String,
    val enabled: Boolean,
    val createdAt: Long,
    val action: String = "block",
    val scope: String = "all_visible",
    val userOrder: Int = 0,
)

/** Một sự kiện app đã chặn; không phải bản sao Call Log hệ thống. */
data class BackupBlockedCall(
    val rawNumber: String,
    val phoneKey: String,
    val blockedAt: Long,
    val ruleType: String,
    val ruleValue: String,
    val consecutiveUnanswered: Int,
    val ruleScope: String = "all_visible",
)

/** Cài đặt hiển thị trong bản sao lưu (mọi trường có thể null nếu bản cũ thiếu). */
data class BackupDisplay(
    val themePref: String?,   // ThemeSettings.Pref (SYSTEM/LIGHT/DARK)
    val langPref: String?,    // LangPref (SYSTEM/VI/EN)
    val fontScale: Float?,
    val smsStrip: Boolean?,
) {
    /** Có ít nhất một cài đặt để áp — nếu toàn null thì coi như không có gì để khôi phục. */
    val hasAny: Boolean get() = themePref != null || langPref != null || fontScale != null || smsStrip != null
}

/** Cài đặt cảnh báo cuộc gọi đi; quyền/vai trò Android không thể và không được sao lưu. */
data class BackupOutgoingCallConfig(
    val enabled: Boolean?,
    val notifyOffNetwork: Boolean?,
    val notifyBlocklist: Boolean?,
    val notifyAllowlist: Boolean?,
    val presentation: String?,
) {
    val hasAny: Boolean
        get() = enabled != null ||
            notifyOffNetwork != null ||
            notifyBlocklist != null ||
            notifyAllowlist != null ||
            presentation != null
}

/**
 * Nội dung một file sao lưu ĐÃ PHÂN TÍCH, giữ trong bộ nhớ (ViewModel) để hiển thị & khôi phục. Mục nào
 * VẮNG trong file thì trường tương ứng = null (khác với "có nhưng rỗng").
 */
data class ParsedBackup(
    val version: Int,
    val appVersion: String,
    val createdAt: Long,
    val templates: List<MessageTemplate>?,
    val qrHistory: List<QrScanEntry>?,
    val categories: List<BackupCategory>?,
    val blockRules: BackupBlockConfig?,
    val blockedCalls: List<BackupBlockedCall>?,
    val myNumbers: List<MyNumberEntry>?,
    val outgoingCall: BackupOutgoingCallConfig?,
    val display: BackupDisplay?,
) {
    /** Các mục THỰC SỰ có trong file (để hiện danh sách chọn khi khôi phục). */
    val present: Set<BackupSection>
        get() = buildSet {
            if (templates != null) add(BackupSection.TEMPLATES)
            if (qrHistory != null) add(BackupSection.QR_HISTORY)
            if (categories != null) add(BackupSection.CATEGORIES)
            if (blockRules != null) add(BackupSection.BLOCK_RULES)
            if (blockedCalls != null) add(BackupSection.BLOCK_HISTORY)
            if (myNumbers != null) add(BackupSection.MY_NUMBER)
            if (outgoingCall != null) add(BackupSection.OUTGOING_CALL)
            if (display != null) add(BackupSection.DISPLAY)
        }

    /** Số mục con của một mảng (mẫu / mã QR / nhóm / số của tôi) — hiển thị cạnh tên mục. */
    fun count(section: BackupSection): Int = when (section) {
        BackupSection.TEMPLATES -> templates?.size ?: 0
        BackupSection.QR_HISTORY -> qrHistory?.size ?: 0
        BackupSection.CATEGORIES -> categories?.size ?: 0
        BackupSection.BLOCK_RULES -> blockRules?.let { it.rules.size + it.numberEntries.size } ?: 0
        BackupSection.BLOCK_HISTORY -> blockedCalls?.size ?: 0
        BackupSection.MY_NUMBER -> myNumbers?.size ?: 0
        BackupSection.OUTGOING_CALL -> if (outgoingCall != null) 1 else 0
        BackupSection.DISPLAY -> if (display != null) 1 else 0
    }
}

/** Số của tôi theo khe SIM trong bản sao lưu. */
data class MyNumberEntry(val slot: Int, val number: String)

/** Lỗi có thể gặp khi sao lưu/khôi phục — ánh xạ sang chuỗi đa ngôn ngữ ở lớp UI. */
enum class BackupError { INVALID_FILE, WRITE_FAILED, READ_FAILED, NOTHING_SELECTED, EMPTY_BACKUP }
