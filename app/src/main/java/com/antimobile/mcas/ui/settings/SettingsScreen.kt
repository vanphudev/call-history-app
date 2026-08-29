package com.antimobile.mcas.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.antimobile.mcas.R
import com.antimobile.mcas.data.agency.AgencyBranch
import com.antimobile.mcas.data.agency.AgencyDataset
import com.antimobile.mcas.data.agency.AgencyProvince
import com.antimobile.mcas.data.messaging.mms.MmsDownloadPolicy
import com.antimobile.mcas.i18n.Lang
import com.antimobile.mcas.i18n.LangPref
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.SimSegmentedToggle
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.ui.theme.ThemeSettings
import com.antimobile.mcas.util.AppLinks
import com.antimobile.mcas.util.FontScaleSettings
import com.antimobile.mcas.util.SimRegistry
import com.antimobile.mcas.util.SimScope
import com.antimobile.mcas.util.SmsSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Màn CÀI ĐẶT: thanh trên (quay lại + tiêu đề) + các NHÓM có tiêu đề căn trái:
 *  - "Ứng dụng": thẻ THÔNG TIN ỨNG DỤNG → mở trang Thông tin ứng dụng hệ thống.
 *  - "Tra cứu cơ quan": thẻ DANH BẠ → bottom sheet chọn TỈNH × NGÀNH (hành chính công / công an).
 *  - "Chính sách & Hỗ trợ": quyền riêng tư, điều khoản (mở màn trong app), trang web, liên hệ.
 *
 * [onOpenStats] mở màn Thống kê chi tiết; [onOpenDirectory] được gọi với tập dữ liệu đã chọn;
 * [onOpenLegal] với "privacy"/"terms".
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenRepeatStats: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenMyNumber: () -> Unit,
    onOpenQrHistory: () -> Unit,
    onOpenFontSize: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenDirectory: (AgencyDataset) -> Unit,
    onOpenLegal: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenCallBlocking: () -> Unit,
    onOpenOutgoingCallSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDonate: () -> Unit
) {
    val context = LocalContext.current
    val s = appStrings()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var showPicker by remember { mutableStateOf(false) }
    var smsStrip by remember { mutableStateOf(SmsSettings.isRemoveDiacritics(context)) }
    var mmsAutoDownload by remember { mutableStateOf(MmsDownloadPolicy.isAutoDownloadEnabled(context)) }
    var mmsRoaming by remember { mutableStateOf(MmsDownloadPolicy.isAutoDownloadWhileRoamingEnabled(context)) }

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
                text = s.common.settings,
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
            // PHẠM VI SIM TOÀN APP (đầu màn) — CHỈ hiện khi máy đang lắp ≥2 SIM (1 SIM thì không có gì để phân).
            // Đồng bộ SIM đang lắp khi mở Cài đặt để thẻ xuất hiện đúng (không cần chờ loadRecent).
            LaunchedEffect(Unit) { withContext(Dispatchers.IO) { SimRegistry.refresh(context) } }
            if (SimRegistry.labels.size >= 2) {
                SectionTitle(s.settings.simScopeSection)
                SimScopeCard()
                Spacer(Modifier.height(22.dp))
            }

            SectionTitle(s.settings.sectionStats)
            DetailedStatsCard(onClick = onOpenStats)
            Spacer(Modifier.height(12.dp))
            RepeatStatsCard(onClick = onOpenRepeatStats)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.category.settingsSection)
            CategoriesCard(onClick = onOpenCategories)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.blocker.settingsSection)
            CallBlockingCard(onClick = onOpenCallBlocking)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.outgoingCall.settingsSection)
            OutgoingCallSettingsCard(onClick = onOpenOutgoingCallSettings)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.settings.sectionMessaging)
            MessageTemplatesCard(onClick = onOpenTemplates)
            Spacer(Modifier.height(12.dp))
            MyNumberCard(onClick = onOpenMyNumber)
            Spacer(Modifier.height(12.dp))
            QrScanHistoryCard(onClick = onOpenQrHistory)
            Spacer(Modifier.height(12.dp))
            MessagingToggleCard(
                icon = Icons.Rounded.Language,
                title = s.settings.smsStripTitle,
                subtitle = s.settings.smsStripSubtitle,
                checked = smsStrip,
                onCheckedChange = { enabled ->
                    smsStrip = enabled
                    SmsSettings.setRemoveDiacritics(context, enabled)
                }
            )
            Spacer(Modifier.height(12.dp))
            MessagingToggleCard(
                icon = Icons.Rounded.MailOutline,
                title = s.settings.mmsAutoDownloadTitle,
                subtitle = s.settings.mmsAutoDownloadSubtitle,
                checked = mmsAutoDownload,
                onCheckedChange = { enabled ->
                    mmsAutoDownload = enabled
                    MmsDownloadPolicy.setAutoDownloadEnabled(context, enabled)
                },
            )
            Spacer(Modifier.height(12.dp))
            MessagingToggleCard(
                icon = Icons.Rounded.SimCard,
                title = s.settings.mmsRoamingTitle,
                subtitle = s.settings.mmsRoamingSubtitle,
                checked = mmsRoaming,
                enabled = mmsAutoDownload,
                onCheckedChange = { enabled ->
                    mmsRoaming = enabled
                    MmsDownloadPolicy.setAutoDownloadWhileRoamingEnabled(context, enabled)
                },
            )

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.settings.sectionAgency)
            AgencyDirectoryCard(onClick = { showPicker = true })

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.settings.sectionDisplay)
            ThemeCard(onClick = onOpenTheme)
            Spacer(Modifier.height(12.dp))
            FontSizeCard(onClick = onOpenFontSize)
            Spacer(Modifier.height(12.dp))
            LanguageCard(onClick = onOpenLanguage)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.backup.settingsSection)
            BackupCard(onClick = onOpenBackup)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.settings.sectionPolicy)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = s.settings.privacyTitle,
                        subtitle = s.settings.privacySubtitle,
                        onClick = { onOpenLegal(LEGAL_PRIVACY) }
                    )
                    SettingRow(
                        icon = Icons.Rounded.Gavel,
                        title = s.settings.termsTitle,
                        subtitle = s.settings.termsSubtitle,
                        onClick = { onOpenLegal(LEGAL_TERMS) }
                    )
                    SettingRow(
                        icon = Icons.Rounded.Language,
                        title = s.settings.websiteTitle,
                        subtitle = AppLinks.SITE.removePrefix("https://").trimEnd('/'),
                        external = true,
                        onClick = { AppLinks.openUrl(context, AppLinks.SITE) }
                    )
                    SettingRow(
                        icon = Icons.Rounded.MailOutline,
                        title = s.settings.contactTitle,
                        subtitle = AppLinks.CONTACT_EMAIL,
                        external = true,
                        onClick = { AppLinks.emailDeveloper(context) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.donate.settingsSection)
            DonateCard(onClick = onOpenDonate)

            Spacer(Modifier.height(22.dp))

            SectionTitle(s.settings.sectionApp)
            AppInfoCard()
        }
    }

    if (showPicker) {
        DatasetPickerSheet(
            onDismiss = { showPicker = false },
            onPick = { dataset ->
                showPicker = false
                onOpenDirectory(dataset)
            }
        )
    }
}

/**
 * Thẻ PHẠM VI SIM TOÀN APP (đầu màn Cài đặt): icon + tiêu đề + giải thích + thanh chọn `SIM 1 · Tất cả · SIM 2`.
 * Chọn một SIM → CẢ app (danh sách/thống kê/chi tiết/cước) chỉ tính & hiển thị SIM đó (lọc tại gốc repo);
 * chọn "Tất cả" → xem mọi SIM, và thanh lọc nhanh theo SIM ở màn danh sách hoạt động trở lại.
 *
 * Ô đang chọn đọc [SimScope.effectiveLabel] (đã chuẩn hoá) nên lựa chọn "lạ" (SIM đã tháo) tự về "Tất cả".
 * Nhãn SIM & nhà mạng suy từ SIM ĐANG LẮP ([SimRegistry], khớp màn Cước), KHÔNG từ nhật ký → không hiện SIM cũ.
 */
@Composable
private fun SimScopeCard() {
    val context = LocalContext.current
    val s = appStrings()
    val labels = SimRegistry.labels
    val carrierLegend = SimRegistry.sims.mapNotNull { sim -> sim.carrier?.let { "${sim.label} · $it" } }
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SimCard, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = s.settings.simScopeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = s.settings.simScopeDesc,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(14.dp))
            // 2 SIM (case chính) → 3 ô căn GIỮA. Nhiều SIM hơn (hiếm) → cho CUỘN ngang để không ô nào bị cắt.
            val toggleRowModifier = if (labels.size > 2) {
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            }
            Row(modifier = toggleRowModifier, horizontalArrangement = Arrangement.Center) {
                SimSegmentedToggle(
                    allLabel = s.settings.simScopeAll,
                    labels = labels,
                    selected = SimScope.effectiveLabel,
                    onSelect = { SimScope.set(context, it) }
                )
            }
            if (carrierLegend.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = carrierLegend.joinToString("     "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** Tiêu đề nhóm căn TRÁI phía trên mỗi thẻ. */
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

/**
 * Thẻ THÔNG TIN ỨNG DỤNG (dạng ngang): icon bo góc + tên & phiên bản + mũi tên.
 * Nhấn vào thẻ để mở trang "Thông tin ứng dụng" của hệ thống (quyền, dung lượng, gỡ cài…).
 */
@Composable
private fun AppInfoCard() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val appIcon = remember {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap(width = 168, height = 168)
                .asImageBitmap()
        }.getOrNull()
    }
    val metaLine = if (versionName.isNotEmpty()) "${appStrings().settings.version(versionName)} · ${AppLinks.AUTHOR}" else AppLinks.AUTHOR

    val openAppDetails: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
        Unit
    }

    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = openAppDetails)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(CardFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.brandTagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.appInfoDesc,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ ỦNG HỘ NHÀ PHÁT TRIỂN (ngay TRÊN thẻ Thông tin ứng dụng): icon trái tim + tiêu đề + phụ đề tự nguyện
 * + mũi tên → mở màn Ủng hộ (mã QR chuyển khoản, chọn số tiền, mở app ngân hàng).
 */
@Composable
private fun DonateCard(onClick: () -> Unit) {
    val s = appStrings().donate
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.cardTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.cardSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = s.open,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ THỐNG KÊ CHI TIẾT: icon biểu đồ + tiêu đề + mô tả 3 hạng mục + mũi tên → mở màn Thống kê chi tiết.
 */
@Composable
private fun DetailedStatsCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().settings.statsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.statsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.statsOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ GỌI LẠI & TRÙNG SỐ: icon lặp + tiêu đề + mô tả + mũi tên → mở màn phân tích số bị gọi lặp lại
 * trong 30 ngày gần nhất (tổng quan, phân bố số lần gọi, chu kỳ quay lại, lịch nhiệt theo số).
 */
@Composable
private fun RepeatStatsCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Repeat, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().settings.repeatTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.repeatSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.repeatOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ MẪU TIN NHẮN: icon SMS + tiêu đề + mô tả + mũi tên → mở màn quản lý mẫu tin nhắn
 * (xem/sửa/xoá mọi mẫu, gửi nhanh, quét QR) — không cần mở một số cụ thể như trước.
 */
@Composable
private fun MessageTemplatesCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Sms, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().settings.templatesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.templatesSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.templatesOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ SỐ ĐIỆN THOẠI CỦA TÔI: icon điện thoại + tiêu đề + mô tả + mũi tên → mở màn nhập/xem số máy theo SIM
 * (dùng cho pattern {phonesim1/2} trong mẫu tin nhắn và "QR của tôi").
 */
@Composable
private fun MyNumberCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().settings.myNumberTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.myNumberSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.myNumberOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ LỊCH SỬ QUÉT MÃ QR: icon quét + tiêu đề + mô tả + mũi tên → mở màn lịch sử các mã QR đã quét gần đây.
 */
@Composable
private fun QrScanHistoryCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().settings.qrHistoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.qrHistorySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.qrHistoryOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ TOGGLE "Gửi SMS không dấu": bật/tắt việc bỏ dấu tiếng Việt & ký tự đặc biệt KHI chuyển nội dung sang
 * ứng dụng nhắn tin (tiết kiệm phí). Cả dòng bấm được để bật/tắt; trạng thái lưu qua [SmsSettings].
 */
@Composable
private fun MessagingToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) Primary else TextSecondary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    checkedBorderColor = Primary
                )
            )
        }
    }
}

/**
 * Thẻ DANH BẠ CƠ QUAN: icon + tiêu đề + mô tả ngành/tỉnh + mũi tên → mở bottom sheet chọn tập.
 */
@Composable
private fun AgencyDirectoryCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().settings.agencyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().settings.agencySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().settings.agencyOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Thẻ SAO LƯU & KHÔI PHỤC: icon + tiêu đề + phụ đề + mũi tên → mở màn sao lưu/khôi phục dữ liệu. */
@Composable
private fun BackupCard(onClick: () -> Unit) {
    val s = appStrings().backup
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SettingsBackupRestore, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.cardTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.cardSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = s.open,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Thẻ PHÂN LOẠI NHÓM: icon + tiêu đề + phụ đề + mũi tên → mở màn danh sách nhóm phân loại. */
@Composable
private fun CategoriesCard(onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Category, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().category.settingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().category.settingsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = appStrings().category.open,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Thẻ CHẶN CUỘC GỌI: đi tới cổng ROLE_CALL_SCREENING rồi danh sách quy tắc/lịch sử. */
@Composable
private fun CallBlockingCard(onClick: () -> Unit) {
    val s = appStrings().blocker
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.settingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.settingsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = s.settingsOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Item độc lập mở Cài đặt cuộc gọi đi; không điều hướng vào cây màn Chặn cuộc gọi. */
@Composable
private fun OutgoingCallSettingsCard(onClick: () -> Unit) {
    val s = appStrings().outgoingCall
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PhoneInTalk,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.settingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.settingsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = s.settingsOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Thẻ CỠ CHỮ: icon cỡ chữ + tiêu đề + mức đang chọn + mũi tên → mở màn chọn cỡ chữ trong app.
 * Phụ đề hiển thị mức hiện tại (đọc snapshot-state nên tự cập nhật khi người dùng đổi ở màn chi tiết).
 */
@Composable
private fun FontSizeCard(onClick: () -> Unit) {
    val s = appStrings()
    val current = FontScaleSettings.scale
    val tier = FontScaleSettings.OPTIONS.firstOrNull { kotlin.math.abs(it.scale - current) < 0.001f }?.tier
        ?: FontScaleSettings.Tier.DEFAULT
    val label = FontScaleSettings.label(tier)
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.fontSize.screenTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$label · ${s.settings.fontSizeCardNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = s.settings.fontSizeOpen,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ GIAO DIỆN: icon chế độ tối + tiêu đề + lựa chọn đang áp (Sáng/Tối/Theo hệ thống) + mũi tên → mở màn chọn.
 * Phụ đề đọc snapshot-state ([ThemeSettings]) nên tự cập nhật khi đổi ở màn chi tiết.
 */
@Composable
private fun ThemeCard(onClick: () -> Unit) {
    val t = appStrings().theme
    val subtitle = when (ThemeSettings.pref) {
        ThemeSettings.Pref.SYSTEM ->
            "${t.optionSystem} · ${if (ThemeSettings.systemDark) t.optionDark else t.optionLight}"
        ThemeSettings.Pref.LIGHT -> t.optionLight
        ThemeSettings.Pref.DARK -> t.optionDark
    }
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t.cardTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Thẻ NGÔN NGỮ: icon dịch + tiêu đề + lựa chọn đang áp + mũi tên → mở màn chọn ngôn ngữ.
 * Phụ đề đọc snapshot-state ([LanguageSettings]) nên tự cập nhật khi đổi ở màn chi tiết.
 */
@Composable
private fun LanguageCard(onClick: () -> Unit) {
    val lang = appStrings().language
    val subtitle = when (LanguageSettings.pref) {
        LangPref.SYSTEM ->
            "${lang.optionSystem} · ${if (LanguageSettings.systemResolved == Lang.VI) lang.vietnamese else lang.english}"
        LangPref.VI -> lang.vietnamese
        LangPref.EN -> lang.english
    }
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Translate, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lang.cardTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Một dòng trong thẻ nhóm (Chính sách & Hỗ trợ). */
@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    external: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = if (external) Icons.AutoMirrored.Rounded.OpenInNew else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(if (external) 18.dp else 22.dp)
        )
    }
}

/** Bottom sheet CHỌN DANH BẠ: nhóm theo tỉnh/thành, mỗi tỉnh có 2 ngành (hành chính công / công an). */
@Composable
private fun DatasetPickerSheet(onDismiss: () -> Unit, onPick: (AgencyDataset) -> Unit) {
    AppBottomSheet(onDismiss = onDismiss, title = appStrings().settings.pickerTitle, showCloseButton = true) { _ ->
        Column(modifier = Modifier.fillMaxWidth()) {
            AgencyProvince.entries.forEach { province ->
                Text(
                    text = province.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp)
                )
                AgencyBranch.entries.forEach { branch ->
                    val dataset = AgencyDataset.of(province, branch)
                    BranchRow(branch = branch, onClick = { onPick(dataset) })
                }
            }
            Text(
                text = appStrings().settings.pickerNote,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun BranchRow(branch: AgencyBranch, onClick: () -> Unit) {
    val icon = if (branch == AgencyBranch.POLICE) Icons.Rounded.LocalPolice else Icons.Rounded.AccountBalance
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = branch.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
