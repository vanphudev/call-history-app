package com.antimobile.mcas.util

import com.antimobile.mcas.data.model.CallType
import com.antimobile.mcas.i18n.appStrings

/**
 * Diễn giải KẾT QUẢ cuộc gọi từ CallLog.Calls.TYPE + DURATION — ĐA NGÔN NGỮ qua [appStrings].
 *
 * Lưu ý (đã kiểm chứng với tài liệu Android): CallLog KHÔNG lưu lý do thất bại, nên một cuộc gọi đi thời
 * lượng = 0 gộp chung nhiều tình huống (không nghe máy / máy bận / ngoài vùng phủ sóng / bị từ chối ở đầu
 * kia) — vì vậy chỉ kết luận chung là "không kết nối", không khẳng định lý do cụ thể.
 */
object CallResults {

    /**
     * Cuộc gọi có ĐÀM THOẠI thật (talk time) hay không — nguồn chân lý DUY NHẤT cho toàn app.
     *
     * QUAN TRỌNG (lỗi thực tế): với MISSED/REJECTED/BLOCKED, cột DURATION của Android KHÔNG phải
     * thời lượng nói chuyện — nhiều máy (Samsung/Xiaomi…) ghi vào đó THỜI GIAN ĐỔ CHUÔNG. Nếu chỉ
     * xét `duration > 0` thì một cuộc gọi nhỡ reo 25s sẽ bị coi là "kết nối thành công 25 giây".
     * Vì vậy nhỡ/từ chối/chặn LUÔN tính là CHƯA kết nối, bất kể duration.
     */
    fun isConnected(type: CallType, durationSeconds: Long): Boolean = when (type) {
        CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> false
        else -> durationSeconds > 0
    }

    /** Nhãn trạng thái ngắn cho mỗi dòng (kiểu "Đã kết nối" / "Không trả lời") — ĐA NGÔN NGỮ qua [appStrings]. */
    fun shortStatus(type: CallType, durationSeconds: Long, isVideo: Boolean = false): String {
        val s = appStrings().callStatus
        val base = when (type) {
            CallType.OUTGOING -> if (durationSeconds > 0) s.connected else s.noResponse
            CallType.INCOMING -> if (durationSeconds > 0) s.connected else s.received
            CallType.MISSED -> s.noAnswer
            CallType.REJECTED -> s.rejected
            CallType.BLOCKED -> s.blocked
            CallType.VOICEMAIL -> s.voicemail
            CallType.ANSWERED_EXTERNALLY -> s.answeredElsewhere
            CallType.UNKNOWN -> s.unknown
        }
        return if (isVideo && isConnected(type, durationSeconds)) "$base (${s.videoLabel})" else base
    }

    /** Câu trạng thái kết nối + thời lượng đầy đủ (dropdown chi tiết / màn "Tất cả") — ĐA NGÔN NGỮ qua [appStrings]. */
    fun connectionLine(type: CallType, durationSeconds: Long): String {
        val s = appStrings().callStatus
        return if (isConnected(type, durationSeconds))
            s.connectionSuccess(TimeFormat.durationLabel(durationSeconds))
        else when (type) {
            CallType.OUTGOING -> s.connectionFailed
            CallType.MISSED -> s.connectionNoAnswer
            CallType.REJECTED -> s.connectionRejected
            CallType.BLOCKED -> s.connectionBlocked
            CallType.VOICEMAIL -> s.connectionVoicemail
            CallType.ANSWERED_EXTERNALLY -> s.connectionExternal
            else -> s.connectionNotConnected
        }
    }
}
