package com.antimobile.mcas.ui.messaging

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.antimobile.mcas.data.messaging.model.ConversationSummary
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.Primary

/** Hội thoại đang được nhấc lên trong context menu, kèm đúng SIM badge đang hiển thị ở danh sách. */
internal data class ConversationContextTarget(
    val conversation: ConversationSummary,
    val bounds: Rect,
    val simLabel: String?,
)

/** Adapter menu mỏng, dùng cùng ContextMenuOverlay và chuyển động với item lịch sử cuộc gọi. */
@Composable
internal fun ConversationContextMenuOverlay(
    target: ConversationContextTarget,
    topInsetPx: Int,
    bottomInsetPx: Int,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit,
    onClosed: () -> Unit,
) {
    val s = appStrings().messaging
    val isUnread = target.conversation.unreadCount > 0
    ContextMenuOverlay(
        bounds = target.bounds,
        actions = listOf(
            ContextAction(
                glyph = ActionGlyph.Vector(
                    icon = if (isUnread) Icons.Rounded.DoneAll else Icons.Rounded.MarkChatUnread,
                    tint = Primary,
                ),
                desc = if (isUnread) s.markRead else s.markUnread,
                onClick = onToggleRead,
            ),
            ContextAction(
                glyph = ActionGlyph.Vector(Icons.Rounded.Delete, AccentRed),
                desc = s.deleteConversation,
                onClick = onDelete,
            ),
        ),
        topInsetPx = topInsetPx,
        bottomInsetPx = bottomInsetPx,
        onClosed = onClosed,
        lifted = {
            ConversationListCard(
                conversation = target.conversation,
                simLabel = target.simLabel,
            )
        },
    )
}
