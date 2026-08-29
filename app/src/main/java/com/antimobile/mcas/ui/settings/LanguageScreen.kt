package com.antimobile.mcas.ui.settings

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
import com.antimobile.mcas.i18n.Lang
import com.antimobile.mcas.i18n.LangPref
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.DividerColor
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary

/**
 * Màn CHỌN NGÔN NGỮ: người dùng chọn 1 trong [LangPref.SYSTEM] / [LangPref.VI] / [LangPref.EN]; áp dụng NGAY
 * cho toàn app (recompose, không khởi động lại — xem [LanguageSettings]). "Theo hệ thống" bám ngôn ngữ máy
 * (chỉ hỗ trợ vi/en; ngôn ngữ khác → tiếng Việt).
 */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings()
    val current = LanguageSettings.pref
    // Ngôn ngữ máy đang quy về (cho phụ đề mục "Theo hệ thống").
    val systemName = if (LanguageSettings.systemResolved == Lang.VI) s.language.vietnamese else s.language.english
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
                text = s.language.cardTitle,
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
            SectionTitle(s.language.sectionChoose)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LanguageRow(
                        title = s.language.optionSystem,
                        subtitle = s.language.currentlyUsing(systemName),
                        selected = current == LangPref.SYSTEM,
                        onClick = { LanguageSettings.set(context, LangPref.SYSTEM) }
                    )
                    RowDivider()
                    LanguageRow(
                        title = s.language.vietnamese,
                        subtitle = null,
                        selected = current == LangPref.VI,
                        onClick = { LanguageSettings.set(context, LangPref.VI) }
                    )
                    RowDivider()
                    LanguageRow(
                        title = s.language.english,
                        subtitle = null,
                        selected = current == LangPref.EN,
                        onClick = { LanguageSettings.set(context, LangPref.EN) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = s.language.note,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Tiêu đề nhóm căn TRÁI (đồng bộ với màn Cài đặt / Cỡ chữ). */
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

/** Một dòng lựa chọn ngôn ngữ: nhãn (+ phụ đề) + nút chọn (radio). Cả dòng bấm được. */
@Composable
private fun LanguageRow(
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
