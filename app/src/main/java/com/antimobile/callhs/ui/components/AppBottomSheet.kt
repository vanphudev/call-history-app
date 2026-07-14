package com.antimobile.callhs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.ProvideAppDensity
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.ui.theme.ThemeSettings
import kotlinx.coroutines.launch

/** Màu thanh tay cầm (drag handle) chuẩn cho mọi bottom sheet toàn dự án. Getter → đổi theo chế độ Sáng/Tối. */
val SheetHandleColor: Color get() = ThemeSettings.colors.sheetHandle

/** Thanh tay cầm nhỏ (36×4, bo tròn) — DÙNG CHUNG cho mọi bottom sheet để đồng nhất (chi tiết + lọc…). */
@Composable
fun SheetHandleBar(modifier: Modifier = Modifier) {
    Box(modifier.width(36.dp).height(4.dp).clip(CircleShape).background(SheetHandleColor))
}

/** Nút ĐÓNG (X) tròn DÙNG CHUNG cho các bottom sheet — bấm để đóng sheet (đóng có hoạt ảnh). */
@Composable
fun SheetCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Close, contentDescription = appStrings().callList.close, tint = TextSecondary, modifier = Modifier.size(22.dp))
    }
}

/**
 * Bottom sheet MODAL DÙNG CHUNG toàn dự án — đồng bộ với sheet ở màn chi tiết: nền trắng
 * ([AppBackground]), bo góc trên, tay cầm [SheetHandleBar], tiêu đề tuỳ chọn, tự chừa thanh điều hướng.
 * [content] nhận sẵn hàm `close` (đóng CÓ HOẠT ẢNH) để gọi sau khi chọn xong một mục.
 *
 * [maxHeightFraction] GIỚI HẠN chiều cao tối đa của sheet theo tỉ lệ màn hình (vd `0.7f` = tối đa 70%):
 * nội dung THẤP hơn ngưỡng → sheet co sát nội dung; CAO hơn ngưỡng → cố định 70% và CUỘN bên trong.
 * MẶC ĐỊNH `0.7f` (mọi sheet cố định tối đa 70%); `null` = không giới hạn.
 *
 * [sheetGesturesEnabled] = false → KHOÁ cử chỉ kéo: vuốt mạnh lên/xuống KHÔNG làm sheet dịch chuyển hay đóng
 * (hết cảnh nhảy/giật). Khi đó vẫn đóng được bằng: nút [showCloseButton] (X góc phải), tap ra ngoài (scrim),
 * hoặc nút back — vì các đường này đi qua `onDismissRequest`, độc lập với cử chỉ kéo. Nội dung bên trong vẫn
 * cuộn dọc bình thường (cuộn nội dung không kéo theo sheet nữa).
 *
 * [showCloseButton] = true → hiện nút đóng (X) ở GÓC PHẢI hàng tiêu đề (ghim trên, không cuộn theo nội dung).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    maxHeightFraction: Float? = 0.7f,
    sheetGesturesEnabled: Boolean = false,
    showCloseButton: Boolean = false,
    // Slot GHIM CỨNG ngay dưới hàng tiêu đề (KHÔNG cuộn theo nội dung); vì nằm trong sheet nên tự di chuyển
    // theo sheet khi kéo lên/xuống. Dùng cho thanh cố định (vd thanh nhóm phân loại ở sheet danh bạ).
    pinnedHeader: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }
    // Chặn trần chiều cao = maxHeightFraction × chiều cao màn (dp). null → Modifier rỗng (không đổi hành vi cũ).
    val heightCap = maxHeightFraction
        ?.let { Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * it).dp) }
        ?: Modifier
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = AppBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                SheetHandleBar()
            }
        }
    ) {
        // Cấp LẠI density theo cỡ chữ người dùng: ModalBottomSheet là cửa sổ Compose riêng nên không kế thừa
        // override ở gốc app (xem [ProvideAppDensity]). Không bọc thì chữ trong sheet kẹt ở 100%.
        ProvideAppDensity {
        Column(modifier = Modifier.fillMaxWidth().then(heightCap)) {
            // Hàng tiêu đề GHIM trên cùng (không cuộn): tiêu đề bên trái + nút đóng (X) góc phải nếu bật.
            if (title != null || showCloseButton) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (showCloseButton) {
                        SheetCloseButton(onClick = close)
                    }
                }
            }
            // Thanh GHIM (không cuộn) — di chuyển theo sheet vì là phần tử của sheet.
            pinnedHeader?.invoke()
            // CUỘN ĐƯỢC khi nội dung cao hơn phần còn lại; weight(fill=false) để nội dung ngắn thì sheet vẫn co sát.
            // verticalScroll đặt trước padding để nav-bar padding nằm trong vùng cuộn, item cuối luôn tới được.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                content(close)
            }
        }
        }
    }
}
