package com.antimobile.callhs.data.messaging.model

/** Hướng của tin SMS trong một hội thoại. */
enum class MessageDirection { INCOMING, OUTGOING }

/** Trạng thái hiển thị thống nhất, tách khỏi các hằng số số nguyên của Telephony Provider. */
enum class MessageState {
    RECEIVED,
    QUEUED,
    SENDING,
    SENT_TO_NETWORK,
    DELIVERED,
    FAILED,
}

/** Một hàng trong danh sách hội thoại SMS. */
data class ConversationSummary(
    val threadId: Long,
    val address: String,
    val displayName: String?,
    val photoUri: String?,
    val snippet: String,
    val timestampMillis: Long,
    val unreadCount: Int,
    val messageCount: Int,
    val lastDirection: MessageDirection,
    val lastState: MessageState,
    val lastSubscriptionId: Int?,
) {
    val title: String get() = displayName?.takeIf(String::isNotBlank) ?: address
    val isNamed: Boolean get() = !displayName.isNullOrBlank()
}

/** Một SMS hiển thị trong màn hội thoại. */
data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestampMillis: Long,
    val direction: MessageDirection,
    val state: MessageState,
    val subscriptionId: Int?,
    val errorCode: Int?,
)

/** SIM đang hoạt động, dùng subId làm định danh; label/slot chỉ phục vụ hiển thị. */
data class MessagingSim(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
    val displayName: String?,
    val carrier: String?,
)

/** Kết quả tính số segment trên đúng nội dung sẽ được gửi. */
data class SmsSegmentInfo(
    val parts: Int,
    val remainingInPart: Int,
    val codeUnitSize: Int,
)

/** Yêu cầu mở trình soạn từ sms:/smsto:/mms:/mmsto: hoặc notification. */
data class MessagingLaunch(
    val recipient: String? = null,
    val body: String = "",
    val threadId: Long? = null,
    val unsupportedMultipleRecipients: Boolean = false,
    val unsupportedMmsPayload: Boolean = false,
    val nonce: Long = System.nanoTime(),
)

sealed interface SendMessageResult {
    data class Queued(val providerId: Long, val threadId: Long) : SendMessageResult
    data class Failed(val reason: SendFailure) : SendMessageResult
}

enum class SendFailure {
    NOT_DEFAULT_APP,
    MISSING_PERMISSION,
    INVALID_RECIPIENT,
    EMPTY_BODY,
    SIM_UNAVAILABLE,
    NO_TELEPHONY,
    PROVIDER_ERROR,
    MODEM_ERROR,
}

