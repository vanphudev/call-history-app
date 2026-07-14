package com.antimobile.callhs.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.util.QrContent

/**
 * Trình bày TRỰC QUAN theo LOẠI mã QR (nhãn loại + icon + màu chữ + màu nền) — DÙNG CHUNG cho sheet xử lý
 * ([QrActionSheet]) và danh sách lịch sử quét (QrScanHistoryScreen). Tách riêng để hai nơi luôn ĐỒNG BỘ
 * icon/màu theo loại, tránh mỗi nơi tự map một kiểu rồi lệch nhau.
 */
data class QrTypePresentation(
    val typeLabel: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

/** Icon + màu + nhãn theo LOẠI mã QR (văn bản dùng icon dòng chữ, khác với các loại có hành động). */
fun qrTypePresentation(content: QrContent): QrTypePresentation {
    val q = appStrings().qr
    return when (content) {
        is QrContent.Web -> QrTypePresentation(q.typeWeb, Icons.Rounded.Language, AccentBlue, AccentBlueBg)
        is QrContent.Phone -> QrTypePresentation(q.typePhone, Icons.Rounded.Phone, AccentGreen, AccentGreenBg)
        is QrContent.Sms -> QrTypePresentation(q.typeSms, Icons.Rounded.Sms, AccentBlue, AccentBlueBg)
        is QrContent.Email -> QrTypePresentation(q.typeEmail, Icons.Rounded.MailOutline, AccentAmber, AccentAmberBg)
        is QrContent.Wifi -> QrTypePresentation(q.typeWifi, Icons.Rounded.Wifi, AccentBlue, AccentBlueBg)
        is QrContent.Contact -> QrTypePresentation(q.typeContact, Icons.Rounded.Person, AccentGreen, AccentGreenBg)
        is QrContent.Geo -> QrTypePresentation(q.typeGeo, Icons.Rounded.Place, AccentRed, AccentRedBg)
        is QrContent.Text -> QrTypePresentation(q.typeText, Icons.Rounded.TextFields, AccentGray, AccentGrayBg)
    }
}

/**
 * Chuỗi XEM TRƯỚC gọn cho một mã QR để hiện làm dòng tiêu đề trong danh sách lịch sử: bỏ tiền tố scheme
 * (tel:/smsto:…), chọn trường tiêu biểu. Rơi về [QrContent.raw] khi loại có cấu trúc mà mọi trường tiêu biểu
 * đều trống.
 */
fun qrPreviewText(content: QrContent): String = when (content) {
    is QrContent.Web -> content.url
    is QrContent.Phone -> content.number
    is QrContent.Sms -> if (content.body.isNullOrBlank()) content.number else "${content.number} · ${content.body}"
    is QrContent.Email -> content.address.ifBlank { content.subject ?: content.raw }
    is QrContent.Wifi -> content.ssid
    is QrContent.Contact -> content.name ?: content.phone ?: content.email ?: content.org ?: content.raw
    is QrContent.Geo -> content.query ?: content.label?.let { "$it · ${content.lat}, ${content.lng}" } ?: "${content.lat}, ${content.lng}"
    is QrContent.Text -> content.raw
}
