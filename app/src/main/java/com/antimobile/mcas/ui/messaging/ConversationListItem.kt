package com.antimobile.mcas.ui.messaging

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.messaging.model.ConversationSummary
import com.antimobile.mcas.data.messaging.model.MessageDirection
import com.antimobile.mcas.data.messaging.model.MessageState
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.category.AvatarCategoryBadges
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.SimBadge
import com.antimobile.mcas.ui.components.rememberPressHighlight
import com.antimobile.mcas.ui.theme.AccentGray
import com.antimobile.mcas.ui.theme.AccentGrayBg
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.TimeFormat
import com.antimobile.mcas.util.SpecialNumbers
import com.antimobile.mcas.util.formatPhone

/**
 * Item hội thoại dùng cùng nhịp thẻ, khoảng cách và thao tác nhấn giữ với [com.antimobile.mcas.ui.calllist.ListCallItem].
 * Khung thẻ được đo theo tọa độ cửa sổ để lớp phủ context menu có thể nhấc đúng bản sao của item.
 */
@Composable
internal fun ConversationListItem(
    conversation: ConversationSummary,
    simLabel: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((Rect) -> Unit)? = null,
    activeInMenu: Boolean = false,
) {
    val cardCoords = remember { ConversationCoordsHolder() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .onGloballyPositioned { cardCoords.value = it }
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f },
    ) {
        ConversationListCard(
            conversation = conversation,
            simLabel = simLabel,
            onClick = onOpen,
            onLongClick = {
                val bounds = cardCoords.value?.takeIf { it.isAttached }?.boundsInWindow()
                if (bounds != null) onLongPress?.invoke(bounds)
            }.takeIf { onLongPress != null },
        )
    }
}

/** Phần thẻ không có padding ngoài; được tái dùng nguyên vẹn làm item nổi trong context menu. */
@Composable
internal fun ConversationListCard(
    conversation: ConversationSummary,
    simLabel: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val failed = conversation.lastState == MessageState.FAILED
    val unread = conversation.unreadCount > 0
    val s = appStrings().messaging
    val conversationStateDescription = listOfNotNull(
        s.unread.takeIf { unread },
        s.stateFailed.takeIf { failed },
    ).joinToString(" · ")
    val messageColor = when {
        failed -> AccentRed
        unread -> Primary
        else -> TextSecondary
    }

    PanelCard(modifier = modifier.fillMaxWidth(), radius = 18.dp) {
        val interaction = remember { MutableInteractionSource() }
        val rowModifier = if (onClick != null) {
            Modifier.fillMaxWidth().combinedClickable(
                interactionSource = interaction,
                indication = rememberPressHighlight(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
        } else {
            Modifier.fillMaxWidth()
        }

        Row(
            modifier = rowModifier
                .semantics {
                    if (conversationStateDescription.isNotEmpty()) {
                        stateDescription = conversationStateDescription
                    }
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(46.dp)) {
                Avatar(
                    label = conversation.title,
                    photoUri = conversation.photoUri,
                    isNamed = conversation.isNamed,
                    size = 46.dp,
                    specialIconRes = SpecialNumbers.of(conversation.address)?.iconRes,
                )
                AvatarCategoryBadges(
                    number = conversation.address,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Message,
                        contentDescription = null,
                        tint = messageColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = conversationTitleLine(conversation),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversationPreviewLine(conversation),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    simLabel?.let { label ->
                        Spacer(Modifier.width(6.dp))
                        SimBadge(label)
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Primary),
                )
                Spacer(Modifier.width(7.dp))
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AccentGrayBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AccentGray,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private class ConversationCoordsHolder(var value: LayoutCoordinates? = null)

private fun conversationTitleLine(conversation: ConversationSummary) = buildAnnotatedString {
    val name = conversation.displayName?.takeIf { it.isNotBlank() }
        ?: SpecialNumbers.name(conversation.address)
    if (name != null) {
        withStyle(SpanStyle(color = TextPrimary)) { append(name) }
        withStyle(SpanStyle(color = TextSecondary)) { append("  ·  ") }
    }
    withStyle(SpanStyle(color = if (name == null) TextPrimary else Primary)) {
        append(formatPhone(conversation.address))
    }
}

private fun conversationPreviewLine(conversation: ConversationSummary) = buildAnnotatedString {
    withStyle(SpanStyle(color = TextSecondary)) {
        append(TimeFormat.dayClock(conversation.timestampMillis))
        append("  ·  ")
        if (conversation.lastDirection == MessageDirection.OUTGOING) {
            append(appStrings().messaging.youPrefix)
            append(" ")
        }
    }
    withStyle(
        SpanStyle(
            color = if (conversation.lastState == MessageState.FAILED) AccentRed else TextSecondary,
            fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
        )
    ) {
        append(conversation.snippet)
    }
}
