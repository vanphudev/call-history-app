package com.antimobile.callhs.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.DividerColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.FontScaleSettings
import kotlin.math.abs

/**
 * Màn CHỌN CỠ CHỮ: người dùng chọn 1 trong các mức [FontScaleSettings.OPTIONS]; áp dụng NGAY cho toàn app
 * (kể cả thẻ XEM TRƯỚC ngay trên màn này). Cỡ chữ này ĐỘC LẬP với hệ thống — máy để 150–200% cũng không
 * ảnh hưởng (xem [com.antimobile.callhs.MainActivity] ghim fontScale = 1.0).
 */
@Composable
fun FontSizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings()
    val current = FontScaleSettings.scale
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = s.common.back,
                    tint = TextPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = s.fontSize.screenTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = navBottom + 24.dp)
        ) {
            SectionTitle(s.fontSize.previewSection)
            PreviewCard()

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.fontSize.chooseSection)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FontScaleSettings.OPTIONS.forEachIndexed { index, option ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DividerColor)
                            )
                        }
                        FontSizeRow(
                            option = option,
                            selected = abs(current - option.scale) < 0.001f,
                            onClick = { FontScaleSettings.set(context, option.scale) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = s.fontSize.note,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Tiêu đề nhóm căn TRÁI (đồng bộ với màn Cài đặt). */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    )
}

/** Một dòng lựa chọn: nhãn + phần trăm cỡ chữ + nút chọn (radio). Cả dòng bấm được. */
@Composable
private fun FontSizeRow(
    option: FontScaleSettings.Option,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = FontScaleSettings.label(option.tier),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(option.scale * 100).toInt()}%" + if (option.scale == FontScaleSettings.DEFAULT) " · ${appStrings().fontSize.default}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = TextSecondary
            )
        )
    }
}

/**
 * Thẻ XEM TRƯỚC: dựng lại một dòng nhật ký cuộc gọi tiêu biểu (avatar + tên + số + thời gian) bằng ĐÚNG
 * các style [MaterialTheme.typography] và màu thật của app → người dùng thấy CHÍNH XÁC giao diện sẽ trông
 * ra sao ở cỡ chữ đang chọn (cả thẻ này cũng phóng theo lựa chọn ngay lập tức).
 */
@Composable
private fun PreviewCard() {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nguyễn Văn An",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().fontSize.sampleSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "0912 345 678",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "10:24",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}
