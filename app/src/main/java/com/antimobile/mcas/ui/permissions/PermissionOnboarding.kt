package com.antimobile.mcas.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.AppToastType
import com.antimobile.mcas.ui.components.rememberPressHighlight
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.CardShadow
import com.antimobile.mcas.ui.theme.DividerColor
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.HairlineBorder
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.util.AppLinks
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.ConsentStore
import com.antimobile.mcas.util.hasPermission

/** Nội dung trực quan của một bước onboarding; [runtimePermission] chỉ có ở ba bước quyền đơn. */
private data class OnboardingStep(
    val runtimePermission: String? = null,
    val stepTitle: String,                     // nhãn ngắn ở chỉ báo bước
    val icon: ImageVector,
    val headline: String,
    val description: String,
    val bullets: List<Pair<ImageVector, String>>
)

/** Kết quả thuần của state machine, tách khỏi Compose để khóa thứ tự onboarding bằng unit test. */
internal data class PermissionOnboardingPosition(
    val currentPermissionIndex: Int,
    val smsStageIndex: Int,
    val consentStageIndex: Int,
    val currentStageIndex: Int,
    val totalSteps: Int,
)

internal fun resolvePermissionOnboardingPosition(
    grantedPermissions: List<Boolean>,
    shouldRequestSms: Boolean,
    smsComplete: Boolean,
    consented: Boolean,
): PermissionOnboardingPosition {
    val currentPermissionIndex = grantedPermissions.indexOfFirst { !it }
    val smsStageIndex = grantedPermissions.size
    val consentStageIndex = grantedPermissions.size + if (shouldRequestSms) 1 else 0
    val currentStageIndex = when {
        currentPermissionIndex >= 0 -> currentPermissionIndex
        shouldRequestSms && !smsComplete -> smsStageIndex
        !consented -> consentStageIndex
        else -> -1
    }
    return PermissionOnboardingPosition(
        currentPermissionIndex = currentPermissionIndex,
        smsStageIndex = smsStageIndex,
        consentStageIndex = consentStageIndex,
        currentStageIndex = currentStageIndex,
        totalSteps = grantedPermissions.size +
            (if (shouldRequestSms) 1 else 0) +
            (if (consented) 0 else 1),
    )
}

/**
 * Ba bước quyền nền tảng được khôi phục từ onboarding gốc:
 * 1) Nhật ký cuộc gọi (READ_CALL_LOG) → 2) Thông tin SIM (READ_PHONE_STATE) → 3) Danh bạ (READ_CONTACTS).
 * Mỗi bước có nội dung giải thích chi tiết + trấn an quyền riêng.
 */
private fun requiredPermissionSteps(): List<OnboardingStep> = with(appStrings().permission) {
    listOf(
        OnboardingStep(
            runtimePermission = Manifest.permission.READ_CALL_LOG,
            stepTitle = callLogStep,
            icon = Icons.Rounded.Phone,
            headline = callLogHeadline,
            description = callLogDesc,
            bullets = listOf(
                Icons.Rounded.Lock to callLogBullet1,
                Icons.Rounded.VerifiedUser to callLogBullet2,
                Icons.Rounded.Visibility to callLogBullet3
            )
        ),
        OnboardingStep(
            runtimePermission = Manifest.permission.READ_PHONE_STATE,
            stepTitle = simStep,
            icon = Icons.Rounded.SimCard,
            headline = simHeadline,
            description = simDesc,
            bullets = listOf(
                Icons.Rounded.SimCard to simBullet1,
                Icons.Rounded.Payments to simBullet2,
                Icons.Rounded.VerifiedUser to simBullet3
            )
        ),
        OnboardingStep(
            runtimePermission = Manifest.permission.READ_CONTACTS,
            stepTitle = contactsStep,
            icon = Icons.Rounded.Person,
            headline = contactsHeadline,
            description = contactsDesc,
            bullets = listOf(
                Icons.Rounded.Person to contactsBullet1,
                Icons.Rounded.Visibility to contactsBullet2,
                Icons.Rounded.VerifiedUser to contactsBullet3
            )
        )
    )
}

/**
 * Bước SMS là một bước giao diện nhưng gồm hai pha hệ thống bắt buộc theo đúng thứ tự:
 * giữ ROLE_SMS trước, sau đó mới xin các quyền SMS/MMS còn thiếu.
 */
private fun smsStep(): OnboardingStep = with(appStrings().permission) {
    OnboardingStep(
        stepTitle = smsStep,
        icon = Icons.Rounded.Sms,
        headline = smsHeadline,
        description = smsDesc,
        bullets = listOf(
            Icons.Rounded.Sms to smsBullet1,
            Icons.Rounded.SimCard to smsBullet2,
            Icons.Rounded.VerifiedUser to smsBullet3,
        ),
    )
}

/**
 * Cổng onboarding BẮT BUỘC, khôi phục luồng toàn màn hình thống nhất của MCAS:
 * Nhật ký cuộc gọi → SIM → Danh bạ → SMS → Điều khoản & quyền riêng tư.
 *
 * Bước SMS được bỏ qua trên thiết bị không có khả năng nhắn tin hoặc không cung cấp ROLE_SMS để ứng dụng
 * không bị khóa vô hạn. Điều khoản luôn là bước cuối và chỉ hiện khi phiên bản hiện tại chưa được đồng ý.
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val permissionSteps = requiredPermissionSteps()
    val shouldRequestSms = SmsRole.isMessagingSupported(context) && SmsRole.isAvailable(context)

    fun permissionSnapshot() = permissionSteps.map { step ->
        step.runtimePermission?.let { hasPermission(context, it) } == true
    }

    var granted by remember { mutableStateOf(permissionSnapshot()) }
    var smsRoleHeld by remember { mutableStateOf(SmsRole.isHeld(context)) }
    var missingCoreSmsPermissions by remember { mutableStateOf(SmsRole.missingCorePermissions(context)) }
    var optionalMmsPermissionsGranted by remember { mutableStateOf(SmsRole.hasOptionalMmsPermissions(context)) }
    var consented by remember { mutableStateOf(ConsentStore.isAccepted(context)) }

    fun refreshSmsState() {
        smsRoleHeld = SmsRole.isHeld(context)
        missingCoreSmsPermissions = SmsRole.missingCorePermissions(context)
        optionalMmsPermissionsGranted = SmsRole.hasOptionalMmsPermissions(context)
    }

    val smsPermissionState = SmsOnboardingPermissionState(
        roleHeld = smsRoleHeld,
        corePermissionsGranted = missingCoreSmsPermissions.isEmpty(),
        optionalMmsPermissionsGranted = optionalMmsPermissionsGranted,
    )
    val smsComplete = isSmsOnboardingComplete(shouldRequestSms, smsPermissionState)
    val position = resolvePermissionOnboardingPosition(
        grantedPermissions = granted,
        shouldRequestSms = shouldRequestSms,
        smsComplete = smsComplete,
        consented = consented,
    )
    val currentPermissionIndex = position.currentPermissionIndex
    var deniedPermissionOnce by remember(currentPermissionIndex) { mutableStateOf(false) }
    var smsRoleDenied by remember { mutableStateOf(false) }
    var smsPermissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = permissionSnapshot()
        if (!isGranted) deniedPermissionOnce = true
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        refreshSmsState()
        if (results.any { !it.value }) smsPermissionDeniedOnce = true
    }
    val smsRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val heldNow = SmsRole.isHeld(context)
        val missingCoreNow = SmsRole.missingCorePermissions(context)
        smsRoleHeld = heldNow
        missingCoreSmsPermissions = missingCoreNow
        optionalMmsPermissionsGranted = SmsRole.hasOptionalMmsPermissions(context)
        smsRoleDenied = !heldNow
        if (heldNow && missingCoreNow.isNotEmpty()) {
            smsPermissionLauncher.launch(missingCoreNow.toTypedArray())
        }
    }

    // Quay lại từ hộp thoại role, Cài đặt ứng dụng hoặc nền hệ thống: luôn lấy trạng thái thật từ Android.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = permissionSnapshot()
        refreshSmsState()
        if (smsRoleHeld) smsRoleDenied = false
    }

    val smsStageIndex = position.smsStageIndex
    val currentStageIndex = position.currentStageIndex
    val totalSteps = position.totalSteps

    if (currentStageIndex < 0) {
        content()
        return
    }

    val currentRuntimePermission = permissionSteps
        .getOrNull(currentPermissionIndex)
        ?.runtimePermission
    val runtimePermanentlyDenied = deniedPermissionOnce && activity != null &&
        currentRuntimePermission != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, currentRuntimePermission)
    val smsPermanentlyDenied = smsRoleHeld && smsPermissionDeniedOnce && activity != null &&
        missingCoreSmsPermissions.any { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

    AnimatedContent(
        targetState = currentStageIndex,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally(tween(300)) { it / 5 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 8 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(300)) { -it / 5 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(220)) { it / 8 } + fadeOut(tween(180)))
            }
        },
        label = "permission-onboarding-step",
    ) { stage ->
        when {
            stage < permissionSteps.size -> {
                val step = permissionSteps[stage]
                PermissionStepScreen(
                    step = step,
                    stepNumber = stage + 1,
                    totalSteps = totalSteps,
                    permanentlyDenied = runtimePermanentlyDenied,
                    onGrant = {
                        val permission = step.runtimePermission ?: return@PermissionStepScreen
                        if (runtimePermanentlyDenied) context.openAppSettings()
                        else permissionLauncher.launch(permission)
                    },
                )
            }

            shouldRequestSms && stage == smsStageIndex -> {
                PermissionStepScreen(
                    step = smsStep(),
                    stepNumber = smsStageIndex + 1,
                    totalSteps = totalSteps,
                    permanentlyDenied = smsPermanentlyDenied,
                    actionLabel = if (smsRoleHeld) {
                        appStrings().messaging.grantPermissions
                    } else {
                        appStrings().messaging.setDefault
                    },
                    supportingMessage = when {
                        smsPermanentlyDenied -> appStrings().permission.deniedMessage
                        smsRoleDenied -> appStrings().permission.smsRoleDeniedMessage
                        else -> null
                    },
                    onGrant = {
                        when {
                            smsPermanentlyDenied -> context.openAppSettings()
                            !smsRoleHeld -> SmsRole.requestIntent(context)?.let(smsRoleLauncher::launch)
                            missingCoreSmsPermissions.isNotEmpty() -> {
                                smsPermissionLauncher.launch(missingCoreSmsPermissions.toTypedArray())
                            }
                        }
                    },
                )
            }

            else -> ConsentStepScreen(
                stepNumber = totalSteps,
                totalSteps = totalSteps,
                onOpenTerms = { AppLinks.openUrl(context, AppLinks.TERMS_URL) },
                onOpenPrivacy = { AppLinks.openUrl(context, AppLinks.PRIVACY_URL) },
                onAccepted = {
                    ConsentStore.accept(context)
                    consented = true
                },
            )
        }
    }
}

@Composable
private fun PermissionStepScreen(
    step: OnboardingStep,
    stepNumber: Int,
    totalSteps: Int,
    permanentlyDenied: Boolean,
    actionLabel: String = appStrings().common.allowAccess,
    supportingMessage: String? = if (permanentlyDenied) appStrings().permission.deniedMessage else null,
    onGrant: () -> Unit
) {
    val grantInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        StepIndicator(current = stepNumber, total = totalSteps)
        Spacer(Modifier.height(6.dp))
        Text(
            text = appStrings().permission.stepIndicator(stepNumber, totalSteps, step.stepTitle),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.size(104.dp).clip(CircleShape).background(AccentGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(step.icon, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = step.headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                step.bullets.forEach { (icon, text) -> PermissionBullet(icon, text) }
            }
        }
        if (supportingMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = supportingMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
                .clip(RoundedCornerShape(28.dp))
                .background(Primary)
                .clickable(
                    interactionSource = grantInteraction,
                    indication = rememberPressHighlight(),
                    role = Role.Button,
                    onClick = onGrant
                )
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (permanentlyDenied) appStrings().common.openSettings else actionLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = appStrings().callList.permRevoke,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Các điểm minh bạch hiển thị ở bước đồng ý điều khoản ("nêu rõ 1 số vấn đề" về cách app hoạt động). */
private fun consentPoints(): List<Pair<ImageVector, String>> = with(appStrings().permission) {
    listOf(
        Icons.Rounded.Lock to consentPoint1,
        Icons.Rounded.VerifiedUser to consentPoint2,
        Icons.Rounded.Public to consentPoint3,
        Icons.Rounded.Payments to consentPoint4
    )
}

/**
 * BƯỚC CUỐI (bắt buộc): nêu rõ cách ứng dụng hoạt động và yêu cầu người dùng TÍCH đồng ý với
 * Điều khoản sử dụng & Chính sách quyền riêng tư trước khi vào ứng dụng. Hai văn bản này MỞ TRÊN WEB
 * (chuyển hướng ra trình duyệt qua [AppLinks.openUrl]) chứ không nhúng/không tải bản XML về màn này.
 * Nút "Đồng ý & vào ứng dụng" chỉ bật khi đã tích — chưa tích thì không vào được app.
 */
@Composable
private fun ConsentStepScreen(
    stepNumber: Int,
    totalSteps: Int,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onAccepted: () -> Unit
) {
    val context = LocalContext.current
    var agreed by rememberSaveable { mutableStateOf(false) }
    val ctaInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        StepIndicator(current = stepNumber, total = totalSteps)
        Spacer(Modifier.height(6.dp))
        Text(
            text = appStrings().permission.stepIndicator(stepNumber, totalSteps, appStrings().permission.consentStepTitle),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.size(104.dp).clip(CircleShape).background(AccentGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Policy, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = appStrings().permission.consentTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = appStrings().permission.consentIntro,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // "Nêu rõ 1 số vấn đề": các điểm minh bạch về cách app hoạt động.
        PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                consentPoints().forEach { (icon, text) -> PermissionBullet(icon, text) }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Hai văn bản pháp lý — NHẤN để MỞ TRÊN WEB (chuyển hướng ra trình duyệt).
        LegalLinkRow(text = appStrings().permission.readTerms, onClick = onOpenTerms)
        Spacer(Modifier.height(10.dp))
        LegalLinkRow(text = appStrings().permission.readPrivacy, onClick = onOpenPrivacy)
        Spacer(Modifier.height(18.dp))

        // Ô TÍCH BẮT BUỘC.
        ConsentCheckRow(agreed = agreed, onToggle = { agreed = !agreed })

        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (agreed) Modifier.shadow(6.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
                    else Modifier
                )
                .clip(RoundedCornerShape(28.dp))
                .background(if (agreed) Primary else FieldSurface)
                // Hiệu ứng nhấn PHẲNG của dự án (không ripple).
                .clickable(
                    interactionSource = ctaInteraction,
                    indication = rememberPressHighlight(),
                    role = Role.Button
                ) {
                    if (agreed) onAccepted()
                    else CallActions.toast(context, appStrings().permission.consentRequired, AppToastType.Warning)
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appStrings().permission.consentAccept,
                color = if (agreed) Color.White else TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = appStrings().permission.consentFooter,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Hàng link mở một văn bản pháp lý TRÊN WEB (icon "mở ngoài"). */
@Composable
private fun LegalLinkRow(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AccentGreenBg)
            // Hiệu ứng nhấn PHẲNG của dự án (không ripple).
            .clickable(
                interactionSource = interaction,
                indication = rememberPressHighlight(),
                role = Role.Button,
                onClickLabel = appStrings().permission.openInBrowser,
                onClick = onClick
            )
            .padding(vertical = 13.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
    }
}

/** Ô TÍCH bắt buộc "Tôi đã đọc và đồng ý…". Nhấn cả hàng để bật/tắt. */
@Composable
private fun ConsentCheckRow(agreed: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // toggleable + Role.Checkbox: TalkBack đọc rõ "ô đánh dấu, đã/chưa chọn".
            // indication = null: KHÔNG phủ hiệu ứng lên cả item khi nhấn tick (theo yêu cầu).
            .toggleable(
                value = agreed,
                interactionSource = interaction,
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (agreed) Primary else FieldSurface)
                .then(if (agreed) Modifier else Modifier.border(1.5.dp, HairlineBorder, RoundedCornerShape(7.dp))),
            contentAlignment = Alignment.Center
        ) {
            if (agreed) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = appStrings().permission.consentCheckLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Chỉ báo tiến trình: các chấm; bước đã qua & đang xét tô XANH (bước hiện tại là chấm dài), còn lại xám. */
@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        for (i in 0 until total) {
            val isCurrent = i == current - 1
            val reached = i <= current - 1
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isCurrent) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (reached) Primary else DividerColor)
            )
            if (i < total - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun PermissionBullet(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(AccentGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { CallActions.toast(this, appStrings().common.appSettingsOpenFailed, AppToastType.Error) }
}
