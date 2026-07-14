package com.antimobile.callhs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.DividerColor
import com.antimobile.callhs.ui.theme.ProvideAppDensity
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary

private val DIALOG_RADIUS = 20.dp
private val BUTTON_ROW_HEIGHT = 54.dp

/** Một nút ở đáy [AppDialog]. */
data class DialogButton(
    val text: String,
    val color: Color,
    val bold: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Khung DIALOG DÙNG CHUNG toàn app (theo mẫu thiết kế): nền trắng bo góc, tiêu đề đậm căn giữa, phần nội
 * dung tuỳ biến ([content] — có thể là đoạn thông báo hoặc ô nhập liệu), một đường kẻ ngang mảnh, rồi
 * HÀNG NÚT chia đều bề rộng (vạch dọc giữa các nút), mỗi nút có RIPPLE lan toả phủ kín.
 *
 * - [dismissOnClickOutside] = false → chạm ra NGOÀI không đóng (yêu cầu: chỉ đóng khi bấm nút / xử lý riêng).
 * - Chạm ra ngoài card chỉ để ẩn bàn phím (clearFocus), không đóng dialog.
 * - Bọc trong Box `imePadding()` → khi bàn phím hiện, dialog tự dời lên phía trên bàn phím.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    buttons: List<DialogButton>,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = false,
    dismissOnBackPress: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        // Dialog là cửa sổ Compose riêng → cấp lại density theo cỡ chữ người dùng (xem [ProvideAppDensity]).
        ProvideAppDensity {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                // Chạm vùng ngoài card → ẩn bàn phím (KHÔNG đóng dialog vì dismissOnClickOutside=false).
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DIALOG_RADIUS))
                    .background(CardSurface)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp)
                )
                if (content != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), content = content)
                    Spacer(Modifier.height(18.dp))
                } else {
                    Spacer(Modifier.height(6.dp))
                }
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth().height(BUTTON_ROW_HEIGHT)) {
                    buttons.forEachIndexed { index, button ->
                        if (index > 0) VerticalDivider(color = DividerColor, thickness = 1.dp)
                        DialogButtonCell(button, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
        }
    }
}

/**
 * Dialog THÔNG BÁO / XÁC NHẬN — cùng khung với [AppDialog], nội dung là một đoạn text xám căn giữa.
 * Kiểu dáng giống hệt dialog nhập liệu, chỉ khác nội dung.
 */
@Composable
fun AppMessageDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    buttons: List<DialogButton>,
    dismissOnClickOutside: Boolean = true
) {
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        buttons = buttons,
        dismissOnClickOutside = dismissOnClickOutside
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DialogButtonCell(button: DialogButton, modifier: Modifier) {
    Box(
        modifier = modifier.clickable(enabled = button.enabled, onClick = button.onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = button.text,
            color = if (button.enabled) button.color else button.color.copy(alpha = 0.4f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (button.bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
