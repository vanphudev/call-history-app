package com.antimobile.callhs.util

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract

/**
 * Theo dõi thay đổi DANH BẠ hệ thống (thêm/sửa/xoá liên hệ, đổi tên/ảnh, đồng bộ tài khoản Google…) rồi
 * gọi [onChange] để màn TỰ nạp lại tên/ảnh liên hệ — SONG SONG với ContentObserver của nhật ký cuộc gọi.
 *
 * VÌ SAO CẦN: danh tính liên hệ (tên/ảnh) được trộn vào nhật ký/danh bạ lúc đọc. Observer của nhật ký chỉ
 * bắt cuộc gọi mới; nếu người dùng CHỈ đổi tên một liên hệ (hoặc tài khoản đồng bộ ngầm cập nhật) mà KHÔNG
 * có cuộc gọi nào phát sinh thì màn đang mở sẽ hiện tên/ảnh CŨ tới khi có sự kiện khác. Observer này lấp đúng
 * khoảng đó, để mọi màn phụ thuộc danh bạ luôn mới ngay cả khi app đang ở tiền cảnh.
 *
 * Đặc điểm:
 *  • GOM NHÓM (debounce [debounceMillis]): một đợt đồng bộ tài khoản có thể bắn HÀNG LOẠT onChange liên
 *    tiếp; ta chỉ nạp lại MỘT lần sau khi lặng, tránh quét lại danh bạ dồn dập gây giật.
 *  • Đăng ký LƯỜI + KIỂM QUYỀN: registerContentObserver mở ContactsProvider nên chỉ gọi SAU khi đã có
 *    READ_CONTACTS (gọi lúc chưa cấp có thể ném SecurityException — đúng bài học của observer nhật ký cuộc
 *    gọi). [ensureRegistered] gọi được nhiều lần: chỉ đăng ký MỘT lần và tự đăng ký khi quyền vừa được cấp.
 *  • Theo dõi trên URI GỐC của authority danh bạ + notifyForDescendants=true để bắt MỌI thay đổi bên dưới
 *    (ContactsProvider2 báo đổi ở URI gốc authority cho hầu hết thao tác ghi).
 *
 * [onChange] được gọi trên luồng CHÍNH (Handler của main Looper) — an toàn để chạm state của Compose.
 */
class ContactsObserver(
    private val context: Context,
    private val debounceMillis: Long = 450L,
    private val onChange: () -> Unit
) {
    private val resolver: ContentResolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private val fire = Runnable {
        // Danh bạ đổi thì ẢNH liên hệ cũng có thể đã đổi dù photoUri giữ nguyên → xoá hiệu lực cache ảnh.
        ContactPhotoSignal.invalidate()
        onChange()
    }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (disposed) return                   // bỏ thông báo còn treo sau khi đã dispose (tránh đăng ký lại)
            handler.removeCallbacks(fire)          // gộp cả đợt onChange → chỉ nạp lại lần cuối
            handler.postDelayed(fire, debounceMillis)
        }
    }
    private var registered = false
    private var disposed = false

    /** Đăng ký nếu CHƯA đăng ký và đã có quyền READ_CONTACTS. An toàn khi gọi lại nhiều lần. */
    fun ensureRegistered() {
        if (registered || disposed) return
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) return
        resolver.registerContentObserver(ContactsContract.AUTHORITY_URI, true, observer)
        registered = true
    }

    /** Huỷ đăng ký + bỏ mọi lần nạp lại đang chờ. Gọi trong ViewModel.onCleared() — KHÔNG dùng lại sau đó. */
    fun dispose() {
        disposed = true
        if (registered) {
            resolver.unregisterContentObserver(observer)
            registered = false
        }
        handler.removeCallbacks(fire)
    }
}
