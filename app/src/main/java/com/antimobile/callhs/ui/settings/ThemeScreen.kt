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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.antimobile.callhs.ui.theme.DividerColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.ui.theme.ThemeSettings

/**
 * Màn CHỌN GIAO DIỆN (Sáng / Tối): người dùng chọn 1 trong [ThemeSettings.Pref] SYSTEM / LIGHT / DARK; áp dụng
 * NGAY cho toàn app (recompose, không khởi động lại — xem [ThemeSettings]). "Theo hệ thống" bám chế độ tối của máy.
 */
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings()
    val current = ThemeSettings.pref
    // Chế độ máy đang quy về (cho phụ đề mục "Theo hệ thống").
    val systemName = if (ThemeSettings.systemDark) s.theme.optionDark else s.theme.optionLight
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
                text = s.theme.cardTitle,
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
            SectionTitle(s.theme.sectionChoose)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeRow(
                        title = s.theme.optionSystem,
                        subtitle = s.theme.currentlyUsing(systemName),
                        selected = current == ThemeSettings.Pref.SYSTEM,
                        onClick = { ThemeSettings.set(context, ThemeSettings.Pref.SYSTEM) }
                    )
                    RowDivider()
                    ThemeRow(
                        title = s.theme.optionLight,
                        subtitle = null,
                        selected = current == ThemeSettings.Pref.LIGHT,
                        onClick = { ThemeSettings.set(context, ThemeSettings.Pref.LIGHT) }
                    )
                    RowDivider()
                    ThemeRow(
                        title = s.theme.optionDark,
                        subtitle = null,
                        selected = current == ThemeSettings.Pref.DARK,
                        onClick = { ThemeSettings.set(context, ThemeSettings.Pref.DARK) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = s.theme.note,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Tiêu đề nhóm căn TRÁI (đồng bộ với màn Cài đặt / Ngôn ngữ). */
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

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .padding(start = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerColor)
    )
}

/** Một dòng lựa chọn giao diện: nhãn (+ phụ đề) + nút chọn (radio). Cả dòng bấm được. */
@Composable
private fun ThemeRow(
    title: String,
    subtitle: String?,
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
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
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
