package com.antimobile.callhs.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.backup.BackupError
import com.antimobile.callhs.data.backup.BackupSection
import com.antimobile.callhs.data.backup.ImportReport
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.i18n.BackupStrings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn SAO LƯU & KHÔI PHỤC dữ liệu app tự quản (mẫu tin nhắn, lịch sử quét QR, phân loại nhóm, số của tôi,
 * cài đặt hiển thị). Hai khối: SAO LƯU (chọn mục → xuất file .json qua SAF) và KHÔI PHỤC (chọn file → chọn
 * mục + chế độ gộp → khôi phục). Lịch sử cuộc gọi là dữ liệu hệ thống chỉ-đọc nên KHÔNG có ở đây.
 */
@Composable
fun BackupScreen(vm: BackupViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings().backup
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Cấu hình khôi phục (mục chọn + chế độ) nằm trong ViewModel → bền qua xoay màn, khớp với [parsed].
    var confirmReplace by remember { mutableStateOf(false) }
    val parsed = vm.parsed

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { vm.export(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { vm.inspect(it, queryDisplayName(context, it)) }
    }

    val exportSelected = vm.exportSelection
    val restoreSelected = vm.restoreSelection

    val runRestore: () -> Unit = {
        if (vm.mode == MergeMode.REPLACE) confirmReplace = true
        else vm.restore(vm.mode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
    ) {
        BackupTopBar(title = s.screenTitle, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = navBottom + 24.dp)
        ) {
            Text(
                text = s.callLogNote,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
            Spacer(Modifier.height(6.dp))

            // ---- Khối SAO LƯU ----
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CardHeader(icon = Icons.Rounded.Backup, title = s.backupTitle, desc = s.backupDesc)
                    Spacer(Modifier.height(14.dp))
                    SubLabel(s.chooseData)
                    BackupSection.entries.forEach { section ->
                        val meta = sectionMeta(section, s)
                        ToggleRow(
                            icon = meta.icon,
                            name = meta.name,
                            sub = meta.sub,
                            checked = vm.exportSel[section] == true,
                            onToggle = { vm.setSection(section, forExport = true, checked = vm.exportSel[section] != true) }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        text = if (vm.busy) s.exporting else s.exportButton,
                        enabled = exportSelected.isNotEmpty() && !vm.busy,
                        loading = vm.busy,
                        onClick = { exportLauncher.launch(suggestedFileName()) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Khối KHÔI PHỤC ----
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CardHeader(icon = Icons.Rounded.Restore, title = s.restoreTitle, desc = s.restoreDesc)
                    Spacer(Modifier.height(14.dp))

                    if (parsed == null) {
                        SecondaryButton(
                            text = s.pickFileButton,
                            enabled = !vm.busy,
                            onClick = { importLauncher.launch(IMPORT_MIME_TYPES) }
                        )
                    } else {
                        // Thông tin file
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardFill)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vm.pickedFileName ?: s.fileLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = s.fileMeta(TimeFormat.dayClock(parsed.createdAt), parsed.appVersion),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        SubLabel(s.chooseSections)
                        parsed.present.sortedBy { it.ordinal }.forEach { section ->
                            val meta = sectionMeta(section, s)
                            ToggleRow(
                                icon = meta.icon,
                                name = meta.name,
                                sub = s.itemsCount(parsed.count(section)),
                                checked = vm.restoreSel[section] == true,
                                onToggle = { vm.setSection(section, forExport = false, checked = vm.restoreSel[section] != true) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        SubLabel(s.modeTitle)
                        ModeRow(vm.mode == MergeMode.REPLACE, s.modeReplace, s.modeReplaceDesc) { vm.mode = MergeMode.REPLACE }
                        ModeRow(vm.mode == MergeMode.ADD, s.modeAdd, s.modeAddDesc) { vm.mode = MergeMode.ADD }
                        ModeRow(vm.mode == MergeMode.UPDATE, s.modeUpdate, s.modeUpdateDesc) { vm.mode = MergeMode.UPDATE }

                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.pickAnotherButton,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = !vm.busy) { importLauncher.launch(IMPORT_MIME_TYPES) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            PrimaryButton(
                                text = if (vm.busy) s.restoring else s.restoreButton,
                                enabled = restoreSelected.isNotEmpty() && !vm.busy,
                                loading = vm.busy,
                                onClick = runRestore,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- Hộp thoại ----
    if (confirmReplace) {
        AppMessageDialog(
            onDismissRequest = { confirmReplace = false },
            title = s.confirmReplaceTitle,
            message = s.confirmReplaceMessage,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { confirmReplace = false },
                DialogButton(s.restoreButton, AccentRed, bold = true) {
                    confirmReplace = false
                    vm.restore(MergeMode.REPLACE)
                }
            )
        )
    }

    if (vm.exportSucceeded) {
        AppMessageDialog(
            onDismissRequest = { vm.dismissExportSuccess() },
            title = s.exportOkTitle,
            message = s.exportOkMessage,
            buttons = listOf(DialogButton(s.done, Primary, bold = true) { vm.dismissExportSuccess() })
        )
    }

    vm.report?.let { report ->
        AppMessageDialog(
            onDismissRequest = { vm.dismissReport() },
            title = s.resultTitle,
            message = reportMessage(report, s),
            buttons = listOf(DialogButton(s.done, Primary, bold = true) { vm.dismissReport() })
        )
    }

    vm.error?.let { err ->
        AppMessageDialog(
            onDismissRequest = { vm.dismissError() },
            title = s.resultTitle,
            message = errorMessage(err, s),
            buttons = listOf(DialogButton(s.done, Primary, bold = true) { vm.dismissError() })
        )
    }
}

// ---------------------------------------------------------------------------------------------------
// Thành phần con
// ---------------------------------------------------------------------------------------------------

@Composable
private fun BackupTopBar(title: String, onBack: () -> Unit) {
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
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardHeader(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}

@Composable
private fun SubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp, top = 2.dp)
    )
}

/** Dòng bật/tắt một mục dữ liệu: icon + tên + phụ đề + ô đánh dấu. Cả dòng bấm được. */
@Composable
private fun ToggleRow(icon: ImageVector, name: String, sub: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            // Padding CÂN ĐỐI 4 phía → vùng nhấn (PressHighlight) ôm nội dung đều nhau trái/phải/trên/dưới,
            // không còn sát mép trái/phải trong khi trên/dưới lại rộng.
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(CardFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        CheckBadge(checked)
    }
}

/** Ô đánh dấu vuông bo góc: nền Primary + dấu tích khi chọn; viền xám khi bỏ chọn. */
@Composable
private fun CheckBadge(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) Primary else CardFill),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** Dòng chọn CHẾ ĐỘ khôi phục (kiểu radio): chấm tròn + tiêu đề + mô tả. */
@Composable
private fun ModeRow(selected: Boolean, title: String, desc: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            // Padding CÂN ĐỐI 4 phía → vùng nhấn (PressHighlight) ôm nội dung đều nhau, không sát mép trái/phải.
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(if (selected) Primary else CardFill),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Primary else Primary.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SecondaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandSoft)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = Primary, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------------------------------
// Tiện ích
// ---------------------------------------------------------------------------------------------------

private data class SectionMeta(val icon: ImageVector, val name: String, val sub: String)

private fun sectionMeta(section: BackupSection, s: BackupStrings): SectionMeta = when (section) {
    BackupSection.TEMPLATES -> SectionMeta(Icons.Rounded.Sms, s.secTemplates, s.secTemplatesSub)
    BackupSection.QR_HISTORY -> SectionMeta(Icons.Rounded.QrCodeScanner, s.secQr, s.secQrSub)
    BackupSection.CATEGORIES -> SectionMeta(Icons.Rounded.Category, s.secCategories, s.secCategoriesSub)
    BackupSection.BLOCK_RULES -> SectionMeta(Icons.Rounded.Block, s.secBlockRules, s.secBlockRulesSub)
    BackupSection.BLOCK_HISTORY -> SectionMeta(Icons.Rounded.History, s.secBlockHistory, s.secBlockHistorySub)
    BackupSection.MY_NUMBER -> SectionMeta(Icons.Rounded.Smartphone, s.secMyNumber, s.secMyNumberSub)
    BackupSection.OUTGOING_CALL -> SectionMeta(Icons.Rounded.PhoneInTalk, s.secOutgoingCall, s.secOutgoingCallSub)
    BackupSection.DISPLAY -> SectionMeta(Icons.Rounded.DarkMode, s.secDisplay, s.secDisplaySub)
}

/** Tóm tắt kết quả khôi phục thành nhiều dòng cho hộp thoại. */
private fun reportMessage(report: ImportReport, s: BackupStrings): String {
    val lines = report.sections.entries.map { (section, r) ->
        val name = sectionMeta(section, s).name
        when (section) {
            BackupSection.DISPLAY,
            BackupSection.OUTGOING_CALL,
            -> "$name: ${if (r.updated > 0) s.displayApplied else s.displayKept}"
            else -> s.resultLine(name, r.added, r.updated, r.skipped)
        }
    }
    val truncated = report.sections.values.any { it.truncated }
    return buildString {
        append(lines.joinToString("\n"))
        if (truncated) {
            append("\n\n")
            append(s.truncatedNote)
        }
    }
}

private fun errorMessage(err: BackupError, s: BackupStrings): String = when (err) {
    BackupError.INVALID_FILE -> s.errInvalidFile
    BackupError.WRITE_FAILED -> s.errWriteFailed
    BackupError.READ_FAILED -> s.errReadFailed
    BackupError.NOTHING_SELECTED -> s.errNothingSelected
    BackupError.EMPTY_BACKUP -> s.errEmptyBackup
}

private val IMPORT_MIME_TYPES = arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")

/** Tên file gợi ý khi xuất: `CallHS-backup-<yyyyMMdd_HHmm>.json`. */
private fun suggestedFileName(): String =
    "CallHS-backup-${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json"

/** Tên hiển thị của file người dùng chọn (OpenableColumns.DISPLAY_NAME), null nếu không đọc được. */
private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
