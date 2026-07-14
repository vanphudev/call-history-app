package com.antimobile.callhs.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.WhatsNewStore

/**
 * Modal THÔNG BÁO CẬP NHẬT hiện ngay khi vào ứng dụng (sau cổng quyền + đồng ý điều khoản).
 *
 * Nội dung KHÔNG khai ở đây mà đọc từ nhật ký phát hành ([com.antimobile.callhs.util.Releases]):
 *  1. Bản nào khai `policyMessage` → modal "Cập nhật chính sách", nút "Đã rõ" và "Xem thêm" (mở Cài đặt).
 *  2. Bản nào khai `features` → modal "Có gì mới" liệt kê các gạch đầu dòng, một nút "Đã rõ".
 *
 * Hai modal KHÔNG BAO GIỜ chồng nhau — chính sách trước, tính năng sau. Mỗi loại chỉ hiện ĐÚNG MỘT LẦN
 * (trạng thái ở [WhatsNewStore]); bản nào không khai gì thì không có modal nào. Người dùng CÀI MỚI đã
 * được đánh dấu "đã xem hết" nên không bị làm phiền.
 *
 * Nhấn nút BACK = "Đã rõ" (vẫn ghi nhận đã xem); chạm ra ngoài KHÔNG đóng.
 */
@Composable
fun UpdateNoticeModals(onOpenSettings: () -> Unit) {
    val context = LocalContext.current

    // Chốt nội dung MỘT lần: sau khi markSeen() thì các hàm này trả về rỗng, không đọc lại được.
    val policyMessage = remember { WhatsNewStore.policyMessageToShow(context) }
    val features = remember { WhatsNewStore.featuresToShow(context) }

    // rememberSaveable (KHÔNG phải remember): xoay màn / đổi cỡ chữ làm Activity dựng lại. Nếu dẫn xuất
    // lại từ prefs, modal đã xử lý xong trong phiên sẽ bung ra lần nữa — chẳng hạn "Có gì mới" đè lên
    // màn Cài đặt vừa mở bằng nút "Xem thêm".
    var showPolicy by rememberSaveable { mutableStateOf(WhatsNewStore.shouldShowPolicy(context)) }
    var showFeatures by rememberSaveable {
        mutableStateOf(!WhatsNewStore.shouldShowPolicy(context) && WhatsNewStore.shouldShowFeatures(context))
    }

    fun closePolicy() {
        WhatsNewStore.markPolicySeen(context)
        showPolicy = false
        // Nối tiếp modal tính năng mới nếu bản này cũng có.
        showFeatures = features.isNotEmpty()
    }

    fun closeFeatures() {
        WhatsNewStore.markFeatureSeen(context)
        showFeatures = false
    }

    if (showPolicy && policyMessage != null) {
        AppMessageDialog(
            onDismissRequest = { closePolicy() },
            dismissOnClickOutside = false,
            title = appStrings().updateNotice.policyTitle,
            message = policyMessage,
            buttons = listOf(
                DialogButton(text = appStrings().updateNotice.gotIt, color = TextSecondary) { closePolicy() },
                // "Xem thêm" CỐ Ý không nối tiếp modal tính năng mới: người dùng đang muốn đi đọc chính
                // sách, bung thêm dialog đè lên màn Cài đặt là quấy rầy. `feature_seen` chưa ghi nên modal
                // đó vẫn còn nguyên và sẽ hiện ở lần mở app kế tiếp.
                DialogButton(text = appStrings().updateNotice.seeMore, color = Primary, bold = true) {
                    WhatsNewStore.markPolicySeen(context)
                    showPolicy = false
                    onOpenSettings()
                }
            )
        )
    } else if (showFeatures && features.isNotEmpty()) {
        AppDialog(
            onDismissRequest = { closeFeatures() },
            dismissOnClickOutside = false,
            title = appStrings().updateNotice.whatsNewTitle,
            buttons = listOf(
                DialogButton(text = appStrings().updateNotice.gotIt, color = Primary, bold = true) { closeFeatures() }
            )
        ) {
            features.forEach { feature -> FeatureBullet(feature) }
        }
    }
}

@Composable
private fun FeatureBullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            text = "•",
            color = Primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
