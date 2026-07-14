package com.antimobile.callhs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.AppLinks
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.ContactActions
import com.antimobile.callhs.util.QrContent

/**
 * Bottom sheet XỬ LÝ MÃ QR CÓ HÀNH ĐỘNG (link web / điện thoại / SMS / email / Wi‑Fi / danh thiếp / bản đồ) —
 * mở khi quét được mã KHÔNG phải văn bản thuần. Hiển thị:
 *  - Loại mã + icon tương ứng (dùng chung [qrTypePresentation]).
 *  - NỘI DUNG gốc đã quét (chọn/bôi được, cuộn nếu dài) hoặc các dòng NHÃN–GIÁ TRỊ với loại có cấu trúc.
 *  - Nút hành động theo loại (mở liên kết / gọi / nhắn tin / gửi email) — bấm xong đóng sheet.
 *  - Nút SAO CHÉP nội dung (giữ sheet mở để có thể làm tiếp).
 *
 * Văn bản thuần KHÔNG mở sheet này (đi theo luồng chọn mẫu chèn `{contextqr}`) — [QrContent.Text] ở đây chỉ là
 * nhánh phòng vệ: chỉ hiện nội dung + nút sao chép, không có hành động riêng.
 */
@Composable
fun QrActionSheet(content: QrContent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val info = presentationOf(content)
    val p = info.presentation

    AppBottomSheet(
        onDismiss = onDismiss,
        title = appStrings().qrHistory.resultSheetTitle,
        maxHeightFraction = 0.7f,
        showCloseButton = true
    ) { close ->
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(p.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(p.icon, contentDescription = null, tint = p.tint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        p.typeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        appStrings().qrAction.scannedContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            val details = info.details
            if (details != null) {
                // Loại có cấu trúc (Wi‑Fi / danh thiếp / bản đồ): hiện các dòng NHÃN – GIÁ TRỊ (bôi/chọn được).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardFill)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    details.forEach { (label, value) -> DetailRow(label, value) }
                }
            } else {
                // Nội dung gốc — bôi/sao chép thủ công được; cuộn dọc nếu dài.
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardFill)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            content.raw.ifBlank { appStrings().qr.empty },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            if (info.action != null && info.actionLabel != null && info.actionIcon != null) {
                SheetActionButton(label = info.actionLabel, icon = info.actionIcon, filled = true) {
                    info.action.invoke(context)
                    close()
                }
                Spacer(Modifier.height(10.dp))
            }
            SheetActionButton(label = appStrings().qrHistory.copyContent, icon = Icons.Rounded.ContentCopy, filled = false) {
                CallActions.copyContent(context, content.raw)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Gói HÀNH ĐỘNG + phần trình bày dùng chung ([QrTypePresentation]) cho từng loại mã QR. */
private class QrInfo(
    val presentation: QrTypePresentation,
    val actionLabel: String?,
    val actionIcon: ImageVector?,
    /** Các dòng NHÃN–GIÁ TRỊ cho loại có cấu trúc; null → hiện nội dung gốc dạng khối văn bản. */
    val details: List<Pair<String, String>>? = null,
    val action: ((android.content.Context) -> Unit)?
)

private fun presentationOf(content: QrContent): QrInfo {
    val p = qrTypePresentation(content)
    return when (content) {
        is QrContent.Web -> QrInfo(
            p, appStrings().qrAction.openLink, Icons.AutoMirrored.Rounded.OpenInNew
        ) { AppLinks.openUrl(it, content.url) }

        is QrContent.Phone -> QrInfo(
            p, appStrings().callDetail.call, Icons.Rounded.Phone
        ) { CallActions.dial(it, content.number) }

        is QrContent.Sms -> QrInfo(
            p, appStrings().callDetail.message, Icons.AutoMirrored.Rounded.Message
        ) { c ->
            if (content.body != null) CallActions.messageWithBody(c, content.number, content.body)
            else CallActions.message(c, content.number)
        }

        is QrContent.Email -> QrInfo(
            p, appStrings().qrAction.sendEmail, Icons.Rounded.MailOutline
        ) { CallActions.sendEmail(it, content.address, content.subject, content.body) }

        is QrContent.Wifi -> QrInfo(
            p, appStrings().qrAction.openWifiSettings, Icons.Rounded.Wifi,
            details = buildList {
                add(appStrings().qrAction.ssid to content.ssid)
                content.password?.let { add(appStrings().qrAction.password to it) }
                content.security?.let { add(appStrings().qrAction.security to wifiSecurityLabel(it)) }
                if (content.hidden) add(appStrings().qrAction.hiddenNetwork to appStrings().qrAction.yes)
            }
        ) { CallActions.openWifiSettings(it) }

        is QrContent.Contact -> QrInfo(
            p, appStrings().qrAction.addToContacts, Icons.Rounded.PersonAdd,
            details = buildList {
                content.name?.let { add(appStrings().qrAction.name to it) }
                content.phone?.let { add(appStrings().qrAction.phone to it) }
                content.email?.let { add("Email" to it) }
                content.org?.let { add(appStrings().qrAction.org to it) }
            }
        ) { ContactActions.addContact(it, content.name, content.phone, content.email, content.org) }

        is QrContent.Geo -> QrInfo(
            p, appStrings().qrAction.openMap, Icons.Rounded.Map,
            details = buildList {
                content.label?.let { add(appStrings().qrAction.label to it) }
                if (content.query != null) {
                    add(appStrings().qrAction.address to content.query)
                } else {
                    add(appStrings().qrAction.latitude to content.lat)
                    add(appStrings().qrAction.longitude to content.lng)
                }
            }
        ) { CallActions.openGeo(it, content.lat, content.lng, content.label, content.query) }

        is QrContent.Text -> QrInfo(p, null, null, action = null)
    }
}

/** Nhãn kiểu bảo mật Wi‑Fi cho dễ đọc; "nopass" → "Không mật khẩu". */
private fun wifiSecurityLabel(t: String): String = when (t.uppercase()) {
    "NOPASS", "NONE", "" -> appStrings().qrAction.wifiNoPassword
    else -> t.uppercase()
}

/** Một dòng NHÃN (trái, nhạt) – GIÁ TRỊ (phải, đậm, bôi chọn được) trong khối chi tiết. */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(120.dp)
        )
        Spacer(Modifier.width(8.dp))
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

/** Nút hành động trong sheet: [filled]=nền xanh chữ trắng (chính); ngược lại nền xám chữ tối (phụ). */
@Composable
private fun SheetActionButton(label: String, icon: ImageVector, filled: Boolean, onClick: () -> Unit) {
    val bg = if (filled) Primary else FieldSurface
    val fg = if (filled) Color.White else TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
