package com.antimobile.callhs.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.antimobile.callhs.i18n.appStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Chia sẻ LIÊN HỆ ra ngoài qua `ACTION_SEND` — Android tự hiện hộp chọn ứng dụng (chia sẻ qua app nào
 * là do NGƯỜI DÙNG chọn, ta KHÔNG chỉ định app đích). Hai dạng:
 *  - [shareText]:   gửi TÊN + SỐ dạng văn bản (`text/plain`).
 *  - [shareBitmap]: gửi ẢNH mã QR đã dựng sẵn (`image/png`) qua FileProvider.
 *
 * Không cần quyền: văn bản đi thẳng; ảnh QR ghi vào cache app rồi cấp quyền đọc TẠM cho app nhận
 * (FLAG_GRANT_READ_URI_PERMISSION). Mọi `startActivity` bọc `runCatching` nên KHÔNG crash.
 */
object ShareContact {

    /** Văn bản chia sẻ: có tên → "Tên\nSố"; không tên → chỉ số (đã định dạng cho dễ đọc). */
    fun buildText(name: String?, number: String): String {
        val phone = formatPhone(number)
        return if (!name.isNullOrBlank()) "$name\n$phone" else phone
    }

    /** Số quay được (không rỗng, không phải marker "-1/-2/-3" của Android); số ẩn thì không chia sẻ. */
    private fun isShareable(number: String): Boolean {
        val trimmed = number.trim()
        return trimmed.isNotEmpty() && !trimmed.startsWith("-")
    }

    /**
     * Gửi TÊN + SỐ dạng văn bản qua hộp chọn ứng dụng. Trả về `false` nếu số bị ẩn/không hợp lệ hoặc
     * không mở được hộp chia sẻ (nơi gọi tự báo cho người dùng).
     */
    fun shareText(context: Context, name: String?, number: String): Boolean {
        if (!isShareable(number)) return false
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildText(name, number))
        }
        return runCatching {
            context.startActivity(Intent.createChooser(send, appStrings().actions.shareContactChooser))
        }.isSuccess
    }

    /**
     * Gửi ẢNH mã QR [bitmap] (đã dựng sẵn — có thể kèm chú thích bên dưới) qua hộp chọn ứng dụng
     * (`image/png`). Ghi cache ở luồng I/O, bắn intent ở luồng Main. Trả về `false` nếu ghi/gửi thất bại.
     *
     * Bọc [NonCancellable]: người dùng bấm chia sẻ là đã quyết định gửi, nên dù bottom sheet rời cây
     * giao diện giữa lúc ghi ảnh (vd CallLog vừa cập nhật) thì việc mở hộp chia sẻ VẪN chạy xong — không
     * bị âm thầm huỷ nửa chừng khiến chạm mà không có gì xảy ra.
     */
    suspend fun shareBitmap(context: Context, bitmap: Bitmap): Boolean = withContext(NonCancellable) {
        val uri: Uri = withContext(Dispatchers.IO) {
            QrShare.cacheForShare(context, bitmap)
        } ?: return@withContext false
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Đưa URI vào clipData để HỘP CHIA SẺ (tiến trình hệ thống) đọc được ảnh xem trước — nếu chỉ
            // để trong EXTRA_STREAM thì phần preview thường trống/hỏng dù app đích vẫn nhận được ảnh.
            clipData = ClipData.newRawUri("Mã QR", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            runCatching { context.startActivity(Intent.createChooser(send, appStrings().actions.shareQrChooser)) }.isSuccess
        }
    }
}
