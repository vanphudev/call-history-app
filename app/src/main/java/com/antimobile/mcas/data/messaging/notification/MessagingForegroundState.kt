package com.antimobile.mcas.data.messaging.notification

/** Chỉ giữ ID không nhạy cảm để receiver biết có nên phát âm cho hội thoại đang mở hay không. */
object MessagingForegroundState {
    @Volatile var visibleThreadId: Long? = null
}

