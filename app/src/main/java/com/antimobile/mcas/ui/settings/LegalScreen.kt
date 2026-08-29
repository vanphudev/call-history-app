package com.antimobile.mcas.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.legal.LegalBlock
import com.antimobile.mcas.data.legal.LegalDocument
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.theme.AccentAmber
import com.antimobile.mcas.ui.theme.AccentAmberBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.AppLinks

const val LEGAL_PRIVACY = "privacy"
const val LEGAL_TERMS = "terms"

/** Đổi ngày ISO `YYYY-MM-DD` sang `DD/MM/YYYY` để hiển thị (khớp với website). Chuỗi khác → giữ nguyên. */
private fun formatLegalDate(raw: String): String {
    val parts = raw.split("-")
    return if (parts.size == 3 && parts[0].length == 4) "${parts[2]}/${parts[1]}/${parts[0]}" else raw
}

/**
 * Màn PHÁP LÝ: Chính sách quyền riêng tư ([LEGAL_PRIVACY]) hoặc Điều khoản sử dụng ([LEGAL_TERMS]).
 * Nội dung tải từ `legal.xml` trên GitHub (cache 30 ngày) — DÙNG CHUNG với website nên luôn đồng nhất.
 * Nếu chưa tải được (offline lần đầu) thì KHÔNG hiện nội dung, chỉ hiện nút "Mở trên web" (không báo lỗi mạng).
 */
@Composable
fun LegalScreen(doc: String, vm: LegalViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isPrivacy = doc != LEGAL_TERMS
    val fallbackTitle = if (isPrivacy) appStrings().settings.privacyTitle else appStrings().settings.termsTitle
    val webUrl = if (isPrivacy) AppLinks.PRIVACY_URL else AppLinks.TERMS_URL

    LaunchedEffect(doc) { vm.load(doc) }
    val loaded = vm.doc

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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appStrings().common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = loaded?.title ?: fallbackTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        when {
            loaded != null -> LoadedContent(loaded, navBottom) { AppLinks.openUrl(context, webUrl) }
            vm.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = Primary)
            }
            else -> OfflineContent { AppLinks.openUrl(context, webUrl) }
        }
    }
}

/** Nội dung đã tải: cập nhật + mở đầu + các mục, kèm nút mở web + liên hệ. */
@Composable
private fun LoadedContent(doc: LegalDocument, navBottom: androidx.compose.ui.unit.Dp, onWeb: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = navBottom + 28.dp)
    ) {
        if (doc.updated.isNotBlank()) {
            Text(
                text = appStrings().legal.lastUpdated(formatLegalDate(doc.updated)),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
        }
        if (doc.intro.isNotBlank()) {
            Text(text = doc.intro, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        doc.sections.forEach { section ->
            if (section.heading.isNotBlank()) H2(section.heading)
            section.blocks.forEach { block ->
                when (block) {
                    is LegalBlock.SubHeading -> SubH(block.text)
                    is LegalBlock.Paragraph -> P(block.text)
                    is LegalBlock.Callout -> Callout(block.text)
                    is LegalBlock.Bullets -> block.items.forEach { Bullet(it) }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        WebButton(onClick = onWeb)
        Spacer(Modifier.height(16.dp))
        Text(
            text = appStrings().legal.contactLine(AppLinks.CONTACT_EMAIL, AppLinks.AUTHOR),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

/** Chưa tải được (offline lần đầu): chỉ hiện nút mở web (không báo lỗi mạng). */
@Composable
private fun OfflineContent(onWeb: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = appStrings().legal.offlineNote,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        WebButton(onClick = onWeb)
    }
}

// ---------------------------------------------------------------------------------------------
// Thành phần văn bản
// ---------------------------------------------------------------------------------------------

@Composable
private fun H2(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SubH(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(3.dp))
}

@Composable
private fun P(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(text = "•  ", style = MaterialTheme.typography.bodyMedium, color = Primary, fontWeight = FontWeight.Bold)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Callout(text: String) {
    Spacer(Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AccentAmberBg)
            .padding(14.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = AccentAmber, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun WebButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandSoft)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = appStrings().legal.openFullWeb, style = MaterialTheme.typography.bodyLarge, color = Primary, fontWeight = FontWeight.SemiBold)
    }
}
