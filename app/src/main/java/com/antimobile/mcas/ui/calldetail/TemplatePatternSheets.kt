package com.antimobile.mcas.ui.calldetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.theme.AccentBlueBg
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.LinkColor
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.SimInfo
import com.antimobile.mcas.util.TemplateContext
import com.antimobile.mcas.util.TemplateFill

/**
 * Bottom sheet GIẢI THÍCH các pattern: mỗi dòng = 1 pattern + mô tả + VÍ DỤ kết quả (điền theo thời điểm
 * hiện tại [nowMillis] và số SIM thật). Mở từ icon "?" cạnh mục "Chèn nhanh" trong màn soạn mẫu.
 */
@Composable
fun PatternHelpSheet(myNumbers: SimInfo.MyNumbers, nowMillis: Long, onDismiss: () -> Unit) {
    val rows = remember(myNumbers, nowMillis, LanguageSettings.lang) {
        val ctx = TemplateContext(
            simNumbers = myNumbers.bySlot,
            qrText = appStrings().templateEditor.qrSampleText,
            nowMillis = nowMillis
        )
        TemplateFill.hints(myNumbers.simCount).map { (token, desc) -> Triple(token, desc, TemplateFill.fill(token, ctx)) }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = appStrings().templateEditor.patternHelpTitle,
        maxHeightFraction = 0.7f,
        sheetGesturesEnabled = false,
        showCloseButton = true
    ) { _ ->
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = appStrings().templateEditor.patternHelpIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
            )
            rows.forEach { (token, desc, example) ->
                PatternHelpRow(token = token, description = desc, example = example.ifBlank { appStrings().templateEditor.patternExampleEmpty })
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PatternHelpRow(token: String, description: String, example: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFill)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BrandSoft).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(token, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = LinkColor)
            }
            Spacer(Modifier.width(10.dp))
            Text("→", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.width(10.dp))
            Text(
                text = example,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

/**
 * Bottom sheet XEM TRƯỚC: hiển thị nội dung SAU KHI điền pattern (dạng bong bóng tin nhắn) để người dùng
 * thấy tin sẽ in ra thế nào. `{contextqr}` hiện dạng placeholder vì chỉ có giá trị thật khi quét lúc gửi.
 */
@Composable
fun TemplatePreviewSheet(rendered: String, hasContextQr: Boolean, onDismiss: () -> Unit) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = appStrings().templateEditor.previewTitle,
        maxHeightFraction = 0.7f,
        sheetGesturesEnabled = false,
        showCloseButton = true
    ) { _ ->
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = appStrings().templateEditor.previewIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(AccentBlueBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = rendered.ifBlank { appStrings().qr.empty },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
            if (hasContextQr) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = appStrings().templateEditor.previewNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
