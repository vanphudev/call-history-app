package com.antimobile.mcas.ui.calllist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.antimobile.mcas.R
import com.antimobile.mcas.data.model.CallEntry
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.TextSecondary

/** Item đang mở context-menu: dữ liệu cuộc gọi + khung THẺ (toạ độ cửa sổ, px) + số cuộc nhỡ liên tiếp. */
data class CallContextTarget(val entry: CallEntry, val bounds: Rect, val missedStreak: Int = 0)

/**
 * Context-menu cho item màn DANH SÁCH cuộc gọi — mỏng, chỉ khai báo 4 hành động (Nhắn tin/Gọi/Zalo/Sao chép)
 * và bản sao thẻ [ListCallCard]; toàn bộ diễn hoạt & bố cục nằm trong [ContextMenuOverlay] dùng chung.
 */
@Composable
fun CallContextMenuOverlay(
    target: CallContextTarget,
    topInsetPx: Int,
    bottomInsetPx: Int,
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onCopy: () -> Unit,
    onZalo: () -> Unit,
    onClosed: () -> Unit,
) {
    val s = appStrings().callList
    ContextMenuOverlay(
        bounds = target.bounds,
        actions = listOf(
            ContextAction(ActionGlyph.Vector(Icons.AutoMirrored.Rounded.Message, AccentBlue), s.menuMessage, onMessage),
            ContextAction(ActionGlyph.Vector(Icons.Rounded.Call, AccentGreen), s.menuCall, onCall),
            ContextAction(ActionGlyph.Logo(R.drawable.ic_zalo), s.menuZalo, onZalo),
            ContextAction(ActionGlyph.Vector(Icons.Rounded.ContentCopy, TextSecondary), s.menuCopy, onCopy),
        ),
        topInsetPx = topInsetPx,
        bottomInsetPx = bottomInsetPx,
        onClosed = onClosed,
        lifted = { ListCallCard(entry = target.entry, missedStreak = target.missedStreak) },
    )
}
