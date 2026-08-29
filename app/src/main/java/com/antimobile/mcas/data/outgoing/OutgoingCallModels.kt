package com.antimobile.mcas.data.outgoing

/** Kiểu hiển thị cảnh báo cuộc gọi đi do người dùng chọn. */
enum class OutgoingCallPresentation(val storageKey: String) {
    HEADS_UP("heads_up"),
    OVERLAY("overlay");

    companion object {
        fun fromStorage(value: String?): OutgoingCallPresentation =
            entries.firstOrNull { it.storageKey == value } ?: HEADS_UP
    }
}

/** Ảnh chụp cài đặt dùng cho một lần đánh giá cuộc gọi đi. */
data class OutgoingCallConfig(
    val enabled: Boolean,
    val notifyOffNetwork: Boolean,
    val notifyBlocklist: Boolean,
    val notifyAllowlist: Boolean,
    val presentation: OutgoingCallPresentation,
) {
    val hasEnabledCondition: Boolean
        get() = notifyOffNetwork || notifyBlocklist || notifyAllowlist
}

/** Thành viên của hai danh sách số chính xác đang hoạt động. */
enum class OutgoingNumberList {
    NONE,
    BLOCKLIST,
    ALLOWLIST,
}

enum class OutgoingCallAlertReason {
    BLOCKLIST,
    OFF_NETWORK,
    ALLOWLIST,
}

data class OutgoingCallAlertDecision(
    val reasons: List<OutgoingCallAlertReason>,
) {
    val shouldAlert: Boolean get() = reasons.isNotEmpty()
}

data class OutgoingCallAlertEvent(
    val number: String,
    val simCarrier: String?,
    val targetCarrier: String?,
    val membership: OutgoingNumberList,
    val reasons: List<OutgoingCallAlertReason>,
    val createdAtMillis: Long,
)

enum class OutgoingCallEventSource {
    /** Preferred API: invoked before Telecom places the call and includes the selected account/SIM. */
    REDIRECTION,
    /** Compatibility path when the user has not granted the dedicated redirection role yet. */
    SCREENING_FALLBACK,
}

/**
 * Policy thuần, không phụ thuộc Android: chỉ cảnh báo ngoại mạng khi biết chắc cả hai nhà mạng.
 * Số cố định/quốc tế/đầu số chưa nhận diện không bị suy đoán thành ngoại mạng.
 */
object OutgoingCallAlertPolicy {
    fun evaluate(
        config: OutgoingCallConfig,
        membership: OutgoingNumberList,
        simCarrier: String?,
        targetCarrier: String?,
    ): OutgoingCallAlertDecision {
        if (!config.enabled || !config.hasEnabledCondition) {
            return OutgoingCallAlertDecision(emptyList())
        }

        val reasons = buildList {
            if (config.notifyBlocklist && membership == OutgoingNumberList.BLOCKLIST) {
                add(OutgoingCallAlertReason.BLOCKLIST)
            }
            if (
                config.notifyOffNetwork &&
                simCarrier != null &&
                targetCarrier != null &&
                !simCarrier.equals(targetCarrier, ignoreCase = true)
            ) {
                add(OutgoingCallAlertReason.OFF_NETWORK)
            }
            if (config.notifyAllowlist && membership == OutgoingNumberList.ALLOWLIST) {
                add(OutgoingCallAlertReason.ALLOWLIST)
            }
        }
        return OutgoingCallAlertDecision(reasons)
    }
}
