package com.antimobile.callhs.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.antimobile.callhs.i18n.appStrings

/**
 * Các hành động phụ trợ — chỉ MỞ app hệ thống qua Intent (không tự gọi, không cần quyền CALL_PHONE),
 * hoặc thao tác clipboard/share. Không can thiệp dữ liệu cuộc gọi.
 */
object CallActions {

    fun dial(context: Context, number: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
        }.onFailure { toast(context, appStrings().common.dialerOpenFailed) }
    }

    /** Mở bàn phím quay số hệ thống (không kèm số) — dùng cho nút FAB. */
    fun openDialer(context: Context) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL))
        }.onFailure { toast(context, appStrings().common.dialerOpenFailed) }
    }

    /** Các nút cho tính năng chưa xây dựng — báo cho người dùng biết sẽ sớm có. */
    fun demo(context: Context) {
        toast(context, appStrings().common.featureComingSoon)
    }

    fun message(context: Context, number: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))))
        }.onFailure { toast(context, appStrings().common.messagingOpenFailed) }
    }

    /**
     * Mở ứng dụng nhắn tin với NGƯỜI NHẬN + NỘI DUNG soạn sẵn (không tự gửi — người dùng bấm gửi).
     * Dùng cho mẫu tin nhắn: chỉ điền vào ô soạn qua extra `sms_body`, KHÔNG cần quyền SEND_SMS.
     */
    fun messageWithBody(context: Context, number: String, body: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)))
            intent.putExtra("sms_body", smsBody(context, body))
            context.startActivity(intent)
        }.onFailure { toast(context, appStrings().common.messagingOpenFailed) }
    }

    /**
     * Nội dung ĐƯA sang app nhắn tin: nếu người dùng bật "Gửi SMS không dấu" ([SmsSettings]) thì bỏ dấu +
     * ký tự Unicode đặc biệt ([SmsText.toGsm7]) để tiết kiệm phí; ngược lại giữ nguyên. Chỉ tác động lúc GỬI —
     * dữ liệu trong ứng dụng không đổi.
     */
    private fun smsBody(context: Context, body: String): String =
        if (SmsSettings.isRemoveDiacritics(context)) SmsText.toGsm7(body) else body

    /**
     * Mở ứng dụng nhắn tin SOẠN MỚI (KHÔNG có người nhận) với NỘI DUNG soạn sẵn — người dùng tự chọn số
     * rồi bấm gửi. Dùng khi gửi mẫu tin nhắn từ màn QUẢN LÝ trong Cài đặt (nơi chưa gắn với số nào).
     * Vẫn qua `ACTION_SENDTO`/`sms_body`, KHÔNG cần quyền SEND_SMS.
     */
    fun composeMessage(context: Context, body: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
            intent.putExtra("sms_body", smsBody(context, body))
            context.startActivity(intent)
        }.onFailure { toast(context, appStrings().common.messagingOpenFailed) }
    }

    fun copy(context: Context, number: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(appStrings().common.phoneNumberLabel, number))
        toast(context, appStrings().common.numberCopied)
    }

    /** Sao chép NỘI DUNG bất kỳ (vd văn bản/QR đã quét) vào clipboard, kèm toast trung tính. */
    fun copyContent(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(appStrings().common.contentLabel, text))
        toast(context, appStrings().common.contentCopied)
    }

    /**
     * Mở ứng dụng email SOẠN SẴN (không tự gửi) tới [address], kèm chủ đề/nội dung nếu có.
     * Dùng cho mã QR loại email (mailto/MATMSG). Qua `ACTION_SENDTO` nên chỉ app email nhận.
     */
    fun sendEmail(context: Context, address: String, subject: String?, body: String?) {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(address)))
            if (!subject.isNullOrEmpty()) intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!body.isNullOrEmpty()) intent.putExtra(Intent.EXTRA_TEXT, body)
            context.startActivity(intent)
        }.onFailure { toast(context, appStrings().common.emailOpenFailed) }
    }

    fun share(context: Context, number: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, number)
        }
        runCatching { context.startActivity(Intent.createChooser(send, appStrings().common.shareNumberTitle)) }
    }

    /** Mở màn CÀI ĐẶT Wi‑Fi hệ thống (từ mã QR Wi‑Fi) — KHÔNG tự kết nối, người dùng tự chọn mạng & nhập mật khẩu. */
    fun openWifiSettings(context: Context) {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                Intent(Settings.Panel.ACTION_WIFI)
            else
                Intent(Settings.ACTION_WIFI_SETTINGS)
            context.startActivity(intent)
        }.onFailure { toast(context, appStrings().common.wifiSettingsOpenFailed) }
    }

    /**
     * Mở ứng dụng bản đồ tại toạ độ [lat],[lng] (từ mã QR geo). Nếu [query] != null (mã chỉ có ĐỊA CHỈ chữ,
     * toạ độ đế 0,0) thì để app bản đồ tự geocode địa chỉ; ngược lại mở đúng toạ độ, kèm [label] nếu có.
     */
    fun openGeo(context: Context, lat: String, lng: String, label: String?, query: String?) {
        runCatching {
            val uri = when {
                !query.isNullOrBlank() -> "geo:$lat,$lng?q=" + Uri.encode(query)
                !label.isNullOrBlank() -> "geo:$lat,$lng?q=" + Uri.encode("$lat,$lng($label)")
                else -> "geo:$lat,$lng?q=$lat,$lng"
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }.onFailure { toast(context, appStrings().common.mapsOpenFailed) }
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
