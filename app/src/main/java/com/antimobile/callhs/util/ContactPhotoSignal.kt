package com.antimobile.callhs.util

import androidx.compose.runtime.mutableIntStateOf

/**
 * Tín hiệu "DANH BẠ vừa đổi" dành cho tầng ẢNH liên hệ. [generation] TĂNG mỗi khi danh bạ máy thay đổi
 * (do [ContactsObserver] gọi [invalidate]).
 *
 * VÌ SAO CẦN: ảnh liên hệ được cache theo photoUri ở cấp tiến trình. Nhưng khi người dùng chỉ THAY ẢNH một
 * liên hệ mà URI ảnh GIỮ NGUYÊN (nhiều máy trả URI thu nhỏ ổn định theo contact_id), cache khoá theo URI sẽ
 * trả lại ảnh CŨ mãi — nạp lại tên/dữ liệu không đủ để ảnh mới xuất hiện. `rememberContactPhoto` đọc
 * [generation] (dạng State) và đưa vào KHOÁ cache ảnh, nên mỗi lần danh bạ đổi thì ảnh được giải mã LẠI dù
 * URI không đổi. Ảnh đang hiển thị được GIỮ trên màn trong lúc giải mã lại nên không chớp.
 */
object ContactPhotoSignal {
    /** Đọc bằng `.intValue` trong composable để tự recompose khi danh bạ đổi. */
    val generation = mutableIntStateOf(0)

    /** Đánh dấu danh bạ đã đổi → buộc giải mã lại ảnh. Gọi trên luồng CHÍNH. */
    fun invalidate() {
        generation.intValue += 1
    }
}
