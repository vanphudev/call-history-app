package com.antimobile.mcas.ui.blocking

import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.blocking.BlockNotificationAlert
import com.antimobile.mcas.data.blocking.BlockNotificationPeriod
import com.antimobile.mcas.data.blocking.BlockNotificationPeriodSettings
import com.antimobile.mcas.data.blocking.BlockNotificationPresentation
import com.antimobile.mcas.data.blocking.BlockNotificationSound
import com.antimobile.mcas.data.blocking.BlockNotificationSoundImportResult
import com.antimobile.mcas.data.blocking.BlockNotificationSoundPreset
import com.antimobile.mcas.data.blocking.CallBlockNotificationSettings
import com.antimobile.mcas.data.blocking.CallBlockNotifier
import com.antimobile.mcas.i18n.Lang
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.components.AppMessageDialog
import com.antimobile.mcas.ui.components.DialogButton
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface NotificationSoundTarget {
    data object Default : NotificationSoundTarget
    data class Period(val value: BlockNotificationPeriod) : NotificationSoundTarget
}

private sealed interface NotificationPresentationTarget {
    data object Default : NotificationPresentationTarget
    data class Period(val value: BlockNotificationPeriod) : NotificationPresentationTarget
}

private data class AdvancedNotificationStrings(
    val screenTitle: String,
    val itemSubtitle: String,
    val defaultSection: String,
    val defaultDescription: String,
    val sound: String,
    val soundOn: String,
    val soundOff: String,
    val vibration: String,
    val vibrationOn: String,
    val vibrationOff: String,
    val presentation: String,
    val presentationStatusBar: String,
    val presentationStatusBarDescription: String,
    val presentationHeadsUp: String,
    val presentationHeadsUpDescription: String,
    val choosePresentation: String,
    val soundFile: String,
    val scheduleSection: String,
    val scheduleTitle: String,
    val scheduleOff: String,
    val scheduleOn: String,
    val scheduleExplanation: String,
    val periodEnabled: String,
    val periodDisabled: String,
    val chooseSound: String,
    val chooseFile: String,
    val chooseFileDescription: String,
    val samplePulse: String,
    val sampleRipple: String,
    val sampleBamboo: String,
    val sampleCrystal: String,
    val customSound: String,
    val morning: String,
    val afternoon: String,
    val evening: String,
    val night: String,
    val importErrorTitle: String,
    val confirm: String,
    val storageError: String,
    val importing: String,
    val notAudio: String,
    val tooLarge: String,
    val tooLong: String,
    val invalidAudio: String,
    val cannotRead: String,
    val cannotKeepPermission: String,
)

private fun advancedNotificationStrings(): AdvancedNotificationStrings =
    if (LanguageSettings.lang == Lang.VI) {
        AdvancedNotificationStrings(
            screenTitle = "Cài đặt thông báo nâng cao",
            itemSubtitle = "Âm thanh, rung và lịch thông báo theo khung giờ",
            defaultSection = "Cấu hình mặc định",
            defaultDescription = "Được dùng khi Lịch thông báo đang tắt.",
            sound = "Âm thanh thông báo",
            soundOn = "Phát âm khi có cuộc gọi bị chặn",
            soundOff = "Thông báo không phát âm",
            vibration = "Rung",
            vibrationOn = "Rung khi có thông báo",
            vibrationOff = "Không rung",
            presentation = "Kiểu hiển thị",
            presentationStatusBar = "Chỉ thanh trạng thái",
            presentationStatusBarDescription = "Chỉ hiện biểu tượng và thông báo trong thanh trạng thái",
            presentationHeadsUp = "Thông báo nổi Heads-up",
            presentationHeadsUpDescription = "Thông báo sổ xuống trên màn hình khi cuộc gọi bị chặn",
            choosePresentation = "Chọn kiểu hiển thị thông báo",
            soundFile = "Âm thông báo",
            scheduleSection = "Lịch thông báo",
            scheduleTitle = "Dùng lịch theo khung giờ",
            scheduleOff = "Đang dùng cấu hình mặc định",
            scheduleOn = "Đang dùng cấu hình riêng của từng khung giờ",
            scheduleExplanation = "Khi bật lịch, cấu hình mặc định không được dùng. App chỉ thông báo trong các khung đang bật bên dưới.",
            periodEnabled = "Có thông báo trong khung giờ này",
            periodDisabled = "Không thông báo trong khung giờ này",
            chooseSound = "Chọn âm thông báo",
            chooseFile = "Chọn file từ thiết bị",
            chooseFileDescription = "Audio tối đa 10 MB và 30 giây",
            samplePulse = "Nhịp MCAS",
            sampleRipple = "Gợn sóng",
            sampleBamboo = "Tre xanh",
            sampleCrystal = "Pha lê",
            customSound = "Âm tùy chỉnh",
            morning = "Sáng",
            afternoon = "Chiều",
            evening = "Tối",
            night = "Đêm",
            importErrorTitle = "Không thể dùng file âm thanh",
            confirm = "Đã hiểu",
            storageError = "Không thể lưu cài đặt. Vui lòng thử lại.",
            importing = "Đang kiểm tra file âm thanh…",
            notAudio = "File đã chọn không phải định dạng âm thanh được hỗ trợ.",
            tooLarge = "File âm thanh phải có dung lượng không quá 10 MB.",
            tooLong = "Âm thanh phải dài không quá 30 giây.",
            invalidAudio = "File âm thanh trống, hỏng hoặc không có thời lượng hợp lệ.",
            cannotRead = "MCAS không thể đọc file đã chọn.",
            cannotKeepPermission = "Thiết bị không cho phép MCAS giữ quyền đọc file này. Hãy chọn file từ nguồn khác.",
        )
    } else {
        AdvancedNotificationStrings(
            screenTitle = "Advanced notification settings",
            itemSubtitle = "Sound, vibration and notification schedule",
            defaultSection = "Default configuration",
            defaultDescription = "Used while Notification schedule is off.",
            sound = "Notification sound",
            soundOn = "Play a sound for a blocked call",
            soundOff = "Notifications are silent",
            vibration = "Vibration",
            vibrationOn = "Vibrate for a notification",
            vibrationOff = "Do not vibrate",
            presentation = "Display style",
            presentationStatusBar = "Status bar only",
            presentationStatusBarDescription = "Show only an icon and notification in the status bar",
            presentationHeadsUp = "Heads-up notification",
            presentationHeadsUpDescription = "Drop the notification over the screen when a call is blocked",
            choosePresentation = "Choose notification display style",
            soundFile = "Alert sound",
            scheduleSection = "Notification schedule",
            scheduleTitle = "Use a time-based schedule",
            scheduleOff = "Using the default configuration",
            scheduleOn = "Using each time period's configuration",
            scheduleExplanation = "When the schedule is on, the default configuration is not used. Notifications are sent only during periods enabled below.",
            periodEnabled = "Notify during this period",
            periodDisabled = "Do not notify during this period",
            chooseSound = "Choose notification sound",
            chooseFile = "Choose a file from device",
            chooseFileDescription = "Audio up to 10 MB and 30 seconds",
            samplePulse = "MCAS Pulse",
            sampleRipple = "Ripple",
            sampleBamboo = "Green Bamboo",
            sampleCrystal = "Crystal",
            customSound = "Custom sound",
            morning = "Morning",
            afternoon = "Afternoon",
            evening = "Evening",
            night = "Night",
            importErrorTitle = "This audio file cannot be used",
            confirm = "Got it",
            storageError = "The setting could not be saved. Please try again.",
            importing = "Checking the audio file…",
            notAudio = "The selected file is not a supported audio format.",
            tooLarge = "The audio file must be no larger than 10 MB.",
            tooLong = "The audio must be no longer than 30 seconds.",
            invalidAudio = "The audio file is empty, damaged, or has no valid duration.",
            cannotRead = "MCAS cannot read the selected file.",
            cannotKeepPermission = "The device did not let MCAS retain access to this file. Choose it from another source.",
        )
    }

@Composable
fun CallBlockNotificationAdvancedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = advancedNotificationStrings()
    val scope = rememberCoroutineScope()
    val config = CallBlockNotificationSettings.config
    var soundTarget by remember { mutableStateOf<NotificationSoundTarget?>(null) }
    var presentationTarget by remember { mutableStateOf<NotificationPresentationTarget?>(null) }
    var pendingImportTarget by remember { mutableStateOf<NotificationSoundTarget?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSoundKey by remember { mutableStateOf<String?>(null) }

    fun stopPreview() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        playingSoundKey = null
    }

    fun saveDefault(alert: BlockNotificationAlert) {
        if (!alert.soundEnabled && playingSoundKey == alert.sound.storageKey) stopPreview()
        if (!CallBlockNotificationSettings.setDefaultAlert(context, alert)) {
            importError = strings.storageError
        }
    }

    fun savePeriod(value: BlockNotificationPeriodSettings) {
        if (
            (!value.enabled || !value.alert.soundEnabled) &&
            playingSoundKey == value.alert.sound.storageKey
        ) stopPreview()
        if (!CallBlockNotificationSettings.setPeriod(context, value)) {
            importError = strings.storageError
        }
    }

    fun setSound(target: NotificationSoundTarget, sound: BlockNotificationSound) {
        if (playingSoundKey != null && playingSoundKey != sound.storageKey) stopPreview()
        when (target) {
            NotificationSoundTarget.Default -> saveDefault(config.defaultAlert.copy(sound = sound))
            is NotificationSoundTarget.Period -> {
                val period = config.period(target.value)
                savePeriod(period.copy(alert = period.alert.copy(sound = sound)))
            }
        }
    }

    fun setPresentation(
        target: NotificationPresentationTarget,
        presentation: BlockNotificationPresentation,
    ) {
        when (target) {
            NotificationPresentationTarget.Default -> saveDefault(
                config.defaultAlert.copy(presentation = presentation)
            )
            is NotificationPresentationTarget.Period -> {
                val period = config.period(target.value)
                savePeriod(period.copy(alert = period.alert.copy(presentation = presentation)))
            }
        }
    }

    fun preview(sound: BlockNotificationSound) {
        if (playingSoundKey == sound.storageKey) {
            stopPreview()
            return
        }
        stopPreview()
        val created = runCatching {
            MediaPlayer.create(context, CallBlockNotifier.notificationSoundUri(context, sound))?.apply {
                setOnCompletionListener { completed ->
                    completed.release()
                    if (player === completed) {
                        player = null
                        playingSoundKey = null
                    }
                }
            }
        }.getOrNull()
        if (created != null) {
            player = created
            playingSoundKey = sound.storageKey
            runCatching { created.start() }.onFailure { stopPreview() }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = pendingImportTarget
        pendingImportTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    CallBlockNotificationSettings.validateAndPersistCustomSound(context, uri)
                }.getOrElse {
                    BlockNotificationSoundImportResult.Error(
                        BlockNotificationSoundImportResult.Reason.CANNOT_READ
                    )
                }
            }
            importing = false
            when (result) {
                is BlockNotificationSoundImportResult.Success -> setSound(target, result.sound)
                is BlockNotificationSoundImportResult.Error -> {
                    importError = when (result.reason) {
                        BlockNotificationSoundImportResult.Reason.NOT_AUDIO -> strings.notAudio
                        BlockNotificationSoundImportResult.Reason.TOO_LARGE -> strings.tooLarge
                        BlockNotificationSoundImportResult.Reason.TOO_LONG -> strings.tooLong
                        BlockNotificationSoundImportResult.Reason.EMPTY_OR_INVALID -> strings.invalidAudio
                        BlockNotificationSoundImportResult.Reason.CANNOT_READ -> strings.cannotRead
                        BlockNotificationSoundImportResult.Reason.CANNOT_KEEP_PERMISSION -> strings.cannotKeepPermission
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { CallBlockNotificationSettings.init(context) }
    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding(),
    ) {
        BlockTopBar(title = strings.screenTitle, onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = navBottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val defaultEnabled = !config.scheduleEnabled
                Column(modifier = Modifier.alpha(if (defaultEnabled) 1f else 0.42f)) {
                    AdvancedSectionTitle(strings.defaultSection)
                    Text(
                        text = strings.defaultDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    )
                    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                        Column {
                            NotificationSwitchRow(
                                title = strings.sound,
                                subtitle = if (config.defaultAlert.soundEnabled) strings.soundOn else strings.soundOff,
                                checked = config.defaultAlert.soundEnabled,
                                icon = if (config.defaultAlert.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                enabled = defaultEnabled,
                                onCheckedChange = { saveDefault(config.defaultAlert.copy(soundEnabled = it)) },
                            )
                            NotificationSwitchRow(
                                title = strings.vibration,
                                subtitle = if (config.defaultAlert.vibrationEnabled) strings.vibrationOn else strings.vibrationOff,
                                checked = config.defaultAlert.vibrationEnabled,
                                icon = Icons.Rounded.Vibration,
                                enabled = defaultEnabled,
                                onCheckedChange = { saveDefault(config.defaultAlert.copy(vibrationEnabled = it)) },
                            )
                            NotificationPresentationRow(
                                title = strings.presentation,
                                presentation = config.defaultAlert.presentation,
                                strings = strings,
                                enabled = defaultEnabled,
                                onClick = {
                                    presentationTarget = NotificationPresentationTarget.Default
                                },
                            )
                            NotificationSoundRow(
                                title = strings.soundFile,
                                soundName = soundName(config.defaultAlert.sound, strings),
                                enabled = defaultEnabled && config.defaultAlert.soundEnabled,
                                isPlaying = playingSoundKey == config.defaultAlert.sound.storageKey,
                                onPreview = { preview(config.defaultAlert.sound) },
                                onClick = { soundTarget = NotificationSoundTarget.Default },
                            )
                        }
                    }
                }
            }

            item {
                AdvancedSectionTitle(strings.scheduleSection)
                PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                    NotificationSwitchRow(
                        title = strings.scheduleTitle,
                        subtitle = if (config.scheduleEnabled) strings.scheduleOn else strings.scheduleOff,
                        checked = config.scheduleEnabled,
                        icon = Icons.Rounded.Schedule,
                        onCheckedChange = {
                            if (it && playingSoundKey == config.defaultAlert.sound.storageKey) {
                                stopPreview()
                            }
                            if (!CallBlockNotificationSettings.setScheduleEnabled(context, it)) {
                                importError = strings.storageError
                            }
                        },
                    )
                }
                AnimatedVisibility(visible = config.scheduleEnabled) {
                    Text(
                        text = strings.scheduleExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                    )
                }
            }

            if (config.scheduleEnabled) {
                BlockNotificationPeriod.entries.forEach { period ->
                    item(key = period.storageKey) {
                        NotificationPeriodCard(
                            value = config.period(period),
                            strings = strings,
                            playingSoundKey = playingSoundKey,
                            onPreview = ::preview,
                            onChoosePresentation = {
                                presentationTarget = NotificationPresentationTarget.Period(period)
                            },
                            onChange = ::savePeriod,
                            onChooseSound = { soundTarget = NotificationSoundTarget.Period(period) },
                        )
                    }
                }
            }

            if (importing) {
                item {
                    Text(
                        text = strings.importing,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    soundTarget?.let { target ->
        val selected = when (target) {
            NotificationSoundTarget.Default -> config.defaultAlert.sound
            is NotificationSoundTarget.Period -> config.period(target.value).alert.sound
        }
        NotificationSoundPicker(
            selected = selected,
            strings = strings,
            playingSoundKey = playingSoundKey,
            onPreview = ::preview,
            onSelect = { sound -> setSound(target, sound) },
            onChooseFile = {
                pendingImportTarget = target
            },
            onDismiss = {
                soundTarget = null
                if (pendingImportTarget != null) fileLauncher.launch(arrayOf("audio/*"))
            },
        )
    }

    presentationTarget?.let { target ->
        val selected = when (target) {
            NotificationPresentationTarget.Default -> config.defaultAlert.presentation
            is NotificationPresentationTarget.Period -> config.period(target.value).alert.presentation
        }
        NotificationPresentationPicker(
            selected = selected,
            strings = strings,
            onSelect = { presentation -> setPresentation(target, presentation) },
            onDismiss = { presentationTarget = null },
        )
    }

    importError?.let { message ->
        AppMessageDialog(
            onDismissRequest = { importError = null },
            title = strings.importErrorTitle,
            message = message,
            buttons = listOf(
                DialogButton(
                    text = strings.confirm,
                    color = Primary,
                    bold = true,
                    onClick = { importError = null },
                )
            ),
        )
    }
}

@Composable
private fun NotificationPresentationRow(
    title: String,
    presentation: BlockNotificationPresentation,
    strings: AdvancedNotificationStrings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.48f)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (presentation == BlockNotificationPresentation.HEADS_UP) {
                    Icons.Rounded.NotificationsActive
                } else {
                    Icons.Rounded.Notifications
                },
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                presentationName(presentation, strings),
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun NotificationPresentationPicker(
    selected: BlockNotificationPresentation,
    strings: AdvancedNotificationStrings,
    onSelect: (BlockNotificationPresentation) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = strings.choosePresentation,
        showCloseButton = true,
    ) { close ->
        BlockNotificationPresentation.entries.forEach { presentation ->
            val isSelected = presentation == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(presentation)
                        close()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) AccentGreenBg else FieldSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) {
                            Icons.Rounded.Check
                        } else if (presentation == BlockNotificationPresentation.HEADS_UP) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.Notifications
                        },
                        contentDescription = null,
                        tint = if (isSelected) AccentGreen else Primary,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        presentationName(presentation, strings),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (presentation == BlockNotificationPresentation.HEADS_UP) {
                            strings.presentationHeadsUpDescription
                        } else {
                            strings.presentationStatusBarDescription
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPeriodCard(
    value: BlockNotificationPeriodSettings,
    strings: AdvancedNotificationStrings,
    playingSoundKey: String?,
    onPreview: (BlockNotificationSound) -> Unit,
    onChoosePresentation: () -> Unit,
    onChange: (BlockNotificationPeriodSettings) -> Unit,
    onChooseSound: () -> Unit,
) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(value.copy(enabled = !value.enabled)) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(
                        if (value.enabled) AccentGreenBg else FieldSurface
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = if (value.enabled) AccentGreen else TextSecondary,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = periodName(value.period, strings),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = periodTime(value.period),
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (value.enabled) strings.periodEnabled else strings.periodDisabled,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = value.enabled,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics { },
                    colors = advancedSwitchColors(),
                )
            }
            AnimatedVisibility(visible = value.enabled) {
                Column {
                    NotificationSwitchRow(
                        title = strings.sound,
                        subtitle = if (value.alert.soundEnabled) strings.soundOn else strings.soundOff,
                        checked = value.alert.soundEnabled,
                        icon = if (value.alert.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        compact = true,
                        onCheckedChange = { onChange(value.copy(alert = value.alert.copy(soundEnabled = it))) },
                    )
                    NotificationSwitchRow(
                        title = strings.vibration,
                        subtitle = if (value.alert.vibrationEnabled) strings.vibrationOn else strings.vibrationOff,
                        checked = value.alert.vibrationEnabled,
                        icon = Icons.Rounded.Vibration,
                        compact = true,
                        onCheckedChange = { onChange(value.copy(alert = value.alert.copy(vibrationEnabled = it))) },
                    )
                    NotificationPresentationRow(
                        title = strings.presentation,
                        presentation = value.alert.presentation,
                        strings = strings,
                        enabled = true,
                        onClick = onChoosePresentation,
                    )
                    NotificationSoundRow(
                        title = strings.soundFile,
                        soundName = soundName(value.alert.sound, strings),
                        enabled = value.alert.soundEnabled,
                        isPlaying = playingSoundKey == value.alert.sound.storageKey,
                        onPreview = { onPreview(value.alert.sound) },
                        onClick = onChooseSound,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .clearAndSetSemantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                stateDescription = subtitle
            }
            .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(if (compact) 40.dp else 48.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(if (compact) 22.dp else 25.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics { },
            colors = advancedSwitchColors(),
        )
    }
}

@Composable
private fun NotificationSoundRow(
    title: String,
    soundName: String,
    enabled: Boolean,
    isPlaying: Boolean,
    onPreview: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.48f)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(soundName, style = MaterialTheme.typography.bodySmall, color = Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onPreview),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = soundName,
                tint = Primary,
                modifier = Modifier.size(25.dp),
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun NotificationSoundPicker(
    selected: BlockNotificationSound,
    strings: AdvancedNotificationStrings,
    playingSoundKey: String?,
    onPreview: (BlockNotificationSound) -> Unit,
    onSelect: (BlockNotificationSound) -> Unit,
    onChooseFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = strings.chooseSound,
        showCloseButton = true,
        maxHeightFraction = 0.82f,
    ) { close ->
        if (selected.customUri != null) {
            SoundChoiceRow(
                title = soundName(selected, strings),
                selected = true,
                isPlaying = playingSoundKey == selected.storageKey,
                onPreview = { onPreview(selected) },
                onClick = close,
            )
        }
        BlockNotificationSoundPreset.entries.forEach { preset ->
            val sound = BlockNotificationSound.preset(preset)
            SoundChoiceRow(
                title = presetName(preset, strings),
                selected = selected == sound,
                isPlaying = playingSoundKey == sound.storageKey,
                onPreview = { onPreview(sound) },
                onClick = {
                    onSelect(sound)
                    close()
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onChooseFile()
                    close()
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = Primary, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.chooseFile, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(strings.chooseFileDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun SoundChoiceRow(
    title: String,
    selected: Boolean,
    isPlaying: Boolean,
    onPreview: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) AccentGreenBg else FieldSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) Icons.Rounded.Check else Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = if (selected) AccentGreen else Primary,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onPreview),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = title,
                tint = Primary,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}

@Composable
private fun AdvancedSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun advancedSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Primary,
    checkedBorderColor = Primary,
)

private fun soundName(sound: BlockNotificationSound, strings: AdvancedNotificationStrings): String =
    sound.preset?.let { presetName(it, strings) }
        ?: sound.displayName
        ?: strings.customSound

private fun presetName(value: BlockNotificationSoundPreset, strings: AdvancedNotificationStrings): String = when (value) {
    BlockNotificationSoundPreset.PULSE -> strings.samplePulse
    BlockNotificationSoundPreset.RIPPLE -> strings.sampleRipple
    BlockNotificationSoundPreset.BAMBOO -> strings.sampleBamboo
    BlockNotificationSoundPreset.CRYSTAL -> strings.sampleCrystal
}

private fun periodName(value: BlockNotificationPeriod, strings: AdvancedNotificationStrings): String = when (value) {
    BlockNotificationPeriod.MORNING -> strings.morning
    BlockNotificationPeriod.AFTERNOON -> strings.afternoon
    BlockNotificationPeriod.EVENING -> strings.evening
    BlockNotificationPeriod.NIGHT -> strings.night
}

private fun periodTime(value: BlockNotificationPeriod): String = when (value) {
    BlockNotificationPeriod.MORNING -> "06:00–12:00"
    BlockNotificationPeriod.AFTERNOON -> "12:00–18:00"
    BlockNotificationPeriod.EVENING -> "18:00–22:00"
    BlockNotificationPeriod.NIGHT -> "22:00–06:00"
}

private fun presentationName(
    value: BlockNotificationPresentation,
    strings: AdvancedNotificationStrings,
): String = when (value) {
    BlockNotificationPresentation.STATUS_BAR -> strings.presentationStatusBar
    BlockNotificationPresentation.HEADS_UP -> strings.presentationHeadsUp
}

internal fun advancedNotificationItemTitle(): String = advancedNotificationStrings().screenTitle
internal fun advancedNotificationItemSubtitle(): String = advancedNotificationStrings().itemSubtitle
