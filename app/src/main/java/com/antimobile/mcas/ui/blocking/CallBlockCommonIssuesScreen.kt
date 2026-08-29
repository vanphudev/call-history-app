package com.antimobile.mcas.ui.blocking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.blocking.CallBlockNotifier
import com.antimobile.mcas.i18n.CallBlockStrings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.rememberPressHighlight
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary

private enum class CommonIssueAction {
    BLOCK_SETTINGS,
    NOTIFICATION_SETTINGS,
}

private data class CommonIssue(
    val id: Int,
    val icon: ImageVector,
    val action: CommonIssueAction?,
)

private val commonIssues = listOf(
    CommonIssue(1, Icons.Rounded.PhoneDisabled, CommonIssueAction.BLOCK_SETTINGS),
    CommonIssue(2, Icons.Rounded.Call, CommonIssueAction.BLOCK_SETTINGS),
    CommonIssue(3, Icons.Rounded.Contacts, CommonIssueAction.BLOCK_SETTINGS),
    CommonIssue(4, Icons.Rounded.NotificationsOff, CommonIssueAction.NOTIFICATION_SETTINGS),
    CommonIssue(5, Icons.AutoMirrored.Rounded.VolumeOff, CommonIssueAction.NOTIFICATION_SETTINGS),
    CommonIssue(6, Icons.Rounded.History, CommonIssueAction.BLOCK_SETTINGS),
    CommonIssue(7, Icons.Rounded.Block, null),
)

/** Hướng dẫn tự kiểm tra các lỗi chặn cuộc gọi phổ biến, không thay đổi cấu hình khi chỉ mở màn. */
@Composable
fun CallBlockCommonIssuesScreen(
    onBack: () -> Unit,
    onOpenBlockSettings: () -> Unit,
) {
    val context = LocalContext.current
    val s = appStrings().blocker
    var expandedIssueId by rememberSaveable { mutableIntStateOf(0) }
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding(),
    ) {
        BlockTopBar(title = s.commonIssuesTitle, onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = navigationBottom + 24.dp,
            ),
        ) {
            item(key = "common-issues-intro") {
                CommonIssuesIntroCard(s)
                Spacer(Modifier.height(12.dp))
            }
            items(commonIssues, key = { it.id }) { issue ->
                CommonIssueCard(
                    issue = issue,
                    expanded = expandedIssueId == issue.id,
                    s = s,
                    onToggle = {
                        expandedIssueId = if (expandedIssueId == issue.id) 0 else issue.id
                    },
                    onAction = {
                        when (issue.action) {
                            CommonIssueAction.BLOCK_SETTINGS -> onOpenBlockSettings()
                            CommonIssueAction.NOTIFICATION_SETTINGS -> {
                                CallBlockNotifier.openNotificationSettings(context)
                            }
                            null -> Unit
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CommonIssuesIntroCard(s: CallBlockStrings) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.commonIssuesSubtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = s.commonIssuesIntro,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun CommonIssueCard(
    issue: CommonIssue,
    expanded: Boolean,
    s: CallBlockStrings,
    onToggle: () -> Unit,
    onAction: () -> Unit,
) {
    PanelCard(
        // AnimatedVisibility bên dưới đã phát chiều cao mới ở từng frame. Không bọc thêm
        // animateContentSize vì hai animation chiều cao nối tiếp nhau làm item kế tiếp đi lên trễ.
        modifier = Modifier.fillMaxWidth(),
        radius = 20.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        stateDescription = if (expanded) s.commonIssuesCollapse else s.commonIssuesExpand
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberPressHighlight(),
                        onClick = onToggle,
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(issue.icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = s.commonIssueTitle(issue.id),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 260),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 220),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(durationMillis = 140)),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    CommonIssueExplanation(
                        label = s.commonIssuesPossibleCause,
                        body = s.commonIssueCause(issue.id),
                        labelColor = AccentRed,
                        background = FieldSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    CommonIssueExplanation(
                        label = s.commonIssuesHowToFix,
                        body = s.commonIssueFix(issue.id),
                        labelColor = AccentGreen,
                        background = AccentGreenBg,
                    )
                    if (issue.action != null) {
                        Spacer(Modifier.height(10.dp))
                        CommonIssueActionButton(
                            text = when (issue.action) {
                                CommonIssueAction.BLOCK_SETTINGS -> s.commonIssuesOpenBlockSettings
                                CommonIssueAction.NOTIFICATION_SETTINGS -> s.commonIssuesOpenNotificationSettings
                            },
                            onClick = onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonIssueExplanation(
    label: String,
    body: String,
    labelColor: Color,
    background: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

@Composable
private fun CommonIssueActionButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(BrandSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberPressHighlight(),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp),
        )
    }
}
