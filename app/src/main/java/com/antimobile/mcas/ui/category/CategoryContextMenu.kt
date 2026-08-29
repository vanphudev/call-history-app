package com.antimobile.mcas.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.antimobile.mcas.data.local.Category
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentRed

/** Nhóm đang mở context-menu: dữ liệu nhóm + khung thẻ (toạ độ cửa sổ, px). */
data class CategoryContextTarget(val category: Category, val bounds: Rect)

/**
 * Context-menu (nhấn giữ) cho hàng nhóm — dựng trên [ContextMenuOverlay] dùng chung, y như CallContextMenu.
 * Nhóm mặc định (builtIn) CHỈ có **Sửa** (không cho xoá); nhóm người dùng có **Sửa** + **Xoá**.
 */
@Composable
fun CategoryContextMenuOverlay(
    target: CategoryContextTarget,
    topInsetPx: Int,
    bottomInsetPx: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClosed: () -> Unit,
) {
    val s = appStrings().category
    val actions = buildList {
        add(ContextAction(ActionGlyph.Vector(Icons.Rounded.Edit, AccentBlue), s.menuEdit, onEdit))
        if (!target.category.isBuiltIn) {
            add(ContextAction(ActionGlyph.Vector(Icons.Rounded.DeleteOutline, AccentRed), s.menuDelete, onDelete))
        }
    }
    ContextMenuOverlay(
        bounds = target.bounds,
        actions = actions,
        topInsetPx = topInsetPx,
        bottomInsetPx = bottomInsetPx,
        onClosed = onClosed,
        lifted = { CategoryRowCard(target.category) },
    )
}
