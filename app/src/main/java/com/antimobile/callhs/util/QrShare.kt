package com.antimobile.callhs.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

/**
 * Tạo, lưu & chia sẻ mã QR "chia sẻ liên hệ". Nội dung mã là URI dạng `tel:<số>` — khi người khác quét
 * bằng camera/ứng dụng QR, máy họ NHẬN RA số điện thoại và mở thẳng trình quay số (họ bấm gọi được ngay,
 * không cần lưu vào danh bạ trước).
 *
 * Chỉ dùng ZXing core (thuần Java) để mã hoá bitmap — KHÔNG cần quyền nào để tạo mã. Lưu ảnh về máy đi
 * qua MediaStore (không cần quyền ghi trên Android 10+, minSdk 29); chia sẻ ảnh đi qua FileProvider
 * (ghi cache app + cấp URI đọc tạm) nên cũng không cần quyền lưu trữ.
 */
object QrShare {

    /** Thư mục con trong Ảnh (Pictures) để gom các ảnh QR đã lưu. */
    private const val ALBUM = "CallHS"

    /** Thư mục con trong cache app để ghi ảnh QR tạm phục vụ chia sẻ (khớp @xml/file_paths). */
    private const val SHARE_DIR = "qr_share"

    /** Số ảnh QR chia sẻ MỚI NHẤT giữ lại trong cache; cũ hơn sẽ bị dọn ở lần chia sẻ sau. */
    private const val SHARE_KEEP = 8

    /**
     * Chuẩn hoá số về payload `tel:` — bỏ khoảng trắng/ký tự trang trí, chỉ giữ chữ số và `+ * #`
     * (các ký tự hợp lệ của một số quay được). Trả về **chuỗi rỗng** khi số KHÔNG tạo được mã gọi thật:
     * số ẩn/không xác định (rỗng) hoặc marker trình bày của Android (`-1` hạn chế, `-2` không rõ,
     * `-3` payphone) — nếu không, `-1` sẽ lọc thành "1" và sinh ra mã QR quay số RÁC. Nơi gọi dựa vào
     * chuỗi rỗng để hiện thông báo thay vì mã QR.
     */
    fun telPayload(number: String): String {
        val trimmed = number.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("-")) return ""
        val cleaned = trimmed.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        return if (cleaned.isEmpty()) "" else "tel:$cleaned"
    }

    /**
     * Mã hoá [content] thành bitmap QR vuông [sizePx]×[sizePx] (module ĐEN trên nền TRẮNG, kèm vùng
     * lặng quanh mã để dễ quét). Ném lỗi nếu nội dung rỗng/không mã hoá được — nơi gọi tự bắt.
     */
    fun encode(content: String, sizePx: Int): Bitmap {
        require(content.isNotEmpty()) { "QR content is empty" }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            // Vùng lặng 4 module theo chuẩn ISO/IEC 18004 — quan trọng cho ẢNH CHIA SẺ/TẢI VỀ: khi người
            // khác xem ảnh sát mép trên nền tối/màu (khung chat), viền trắng đủ rộng mới quét được ổn định.
            EncodeHintType.MARGIN to 4,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    /**
     * Ghép [lines] (tối đa vài dòng, ví dụ "Di động: …" / "Nhà mạng: …") vào PHÍA DƯỚI [qr] thành một
     * ảnh MỚI (nền trắng, chữ đen căn giữa) — dùng cho "QR của tôi" để ảnh HIỂN THỊ và ảnh TẢI/CHIA SẺ
     * đều mang sẵn thông tin. Bỏ qua dòng rỗng; nếu không còn dòng nào thì trả về [qr] nguyên bản.
     * Tự thu nhỏ cỡ chữ nếu dòng dài nhất vượt bề ngang.
     */
    fun renderCaptioned(qr: Bitmap, lines: List<String>): Bitmap {
        val texts = lines.filter { it.isNotBlank() }
        if (texts.isEmpty()) return qr
        val w = qr.width
        val sidePad = w * 0.06f
        val topGap = w * 0.03f
        val bottomPad = w * 0.06f
        val lineGap = w * 0.02f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = w * 0.05f
        }
        // Thu nhỏ cỡ chữ nếu dòng dài nhất vượt bề ngang cho phép (giữ chữ luôn nằm trọn trong ảnh).
        val maxTextWidth = w - 2 * sidePad
        val widest = texts.maxOf { paint.measureText(it) }
        if (widest > maxTextWidth) paint.textSize *= (maxTextWidth / widest)
        val fm = paint.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val captionH = topGap + texts.size * lineHeight + (texts.size - 1) * lineGap + bottomPad
        val out = Bitmap.createBitmap(w, qr.height + captionH.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(qr, 0f, 0f, null)
        var baseline = qr.height + topGap - fm.ascent
        for (t in texts) {
            canvas.drawText(t, w / 2f, baseline, paint)
            baseline += lineHeight + lineGap
        }
        return out
    }

    /**
     * Lưu [bitmap] vào thư viện ảnh (Pictures/CallHS) qua MediaStore. Không cần quyền ghi trên
     * Android 10+ (minSdk 29 nên luôn đi nhánh này). Trả về [Uri] ảnh đã lưu, hoặc `null` nếu thất bại.
     * Gọi trên luồng nền (I/O). Nếu ghi hỏng giữa chừng thì DỌN bản ghi treo để không để lại ảnh rỗng.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("PNG compress failed")
                }
            } ?: throw IllegalStateException("openOutputStream returned null")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            runCatching { resolver.delete(uri, null, null) } // dọn bản ghi treo (IS_PENDING) khi lỗi
            null
        }
    }

    /**
     * Ghi [bitmap] ra file PNG trong cache app (thư mục con [SHARE_DIR]) để CHIA SẺ tạm qua ACTION_SEND,
     * trả về `content://` URI do FileProvider cấp (app nhận đọc được nhờ FLAG_GRANT_READ_URI_PERMISSION),
     * hoặc `null` nếu ghi lỗi. Gọi trên luồng nền (I/O).
     *
     * Mỗi lần chia sẻ dùng TÊN FILE DUY NHẤT (theo mốc thời gian): nếu dùng tên cố định, URI content://
     * sinh ra y hệt nhau giữa các liên hệ khác nhau → app nhận (hoặc cache thumbnail của hệ thống) có
     * thể hiện lại ảnh QR CŨ của liên hệ trước. Ảnh cũ được [pruneStale] dọn dần nên không phình cache.
     */
    fun cacheForShare(context: Context, bitmap: Bitmap): Uri? {
        return runCatching {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            pruneStale(dir)
            val file = File(dir, "contact_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("PNG compress failed")
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    /**
     * Giữ lại [SHARE_KEEP] ảnh chia sẻ MỚI NHẤT, xoá phần còn lại (best-effort). Giữ theo SỐ LƯỢNG thay vì
     * theo tuổi để lần chia sẻ TRƯỚC (app nhận có thể đọc trễ, vd gửi lúc offline) không bị xoá mất file.
     */
    private fun pruneStale(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= SHARE_KEEP) return
        files.sortedByDescending { it.lastModified() }.drop(SHARE_KEEP).forEach { runCatching { it.delete() } }
    }
}
