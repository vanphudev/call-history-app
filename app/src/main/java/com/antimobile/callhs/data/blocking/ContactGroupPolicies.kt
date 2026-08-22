package com.antimobile.callhs.data.blocking

/** Chính sách cấp nhóm dành cho số đã lưu trong Danh bạ. */
enum class SavedContactGroupPolicy {
    FOLLOW_ADVANCED,
    ALLOW,
    BLOCK,
}

/** Chính sách cấp nhóm dành cho số điện thoại chưa lưu trong Danh bạ. */
enum class UnknownNumberPolicy {
    PASS,
    BLOCK_ALWAYS,
    BLOCK_UNTIL_REPEAT,
}
