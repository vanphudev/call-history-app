package com.antimobile.callhs.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.local.Category
import com.antimobile.callhs.data.local.CategoryCatalog
import com.antimobile.callhs.data.local.CategoryRepository
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.rememberPressHighlight
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import kotlinx.coroutines.launch

/** Nhóm đang mở context-menu ở màn danh sách: nhóm + khung thẻ (toạ độ cửa sổ, px). */
private data class ListContextTarget(val category: Category, val bounds: Rect)

/**
 * Màn DANH SÁCH nhóm phân loại (mở từ Cài đặt). Đọc [CategoryCatalog] (phản ứng) nên tự cập nhật sau khi
 * tạo/sửa/xoá. NHẤN GIỮ mở context-menu Sửa/Xoá (nhóm mặc định chỉ có Sửa). CHẠM hàng → mở màn sửa.
 * Nút "Tạo nhóm" nổi đáy màn; chặn khi đã đủ 5 nhóm.
 */
@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { CategoryRepository(context) }
    val density = LocalDensity.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navBarPx = WindowInsets.navigationBars.getBottom(density)
    val s = appStrings().category

    val categories = CategoryCatalog.categories
    var contextTarget by remember { mutableStateOf<ListContextTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding()
        ) {
            ListTopBar(count = categories.size, onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = navBottom + 92.dp)
            ) {
                item { ListHint() }
                items(categories, key = { it.id }) { category ->
                    CategoryListItem(
                        category = category,
                        onClick = { onEdit(category.id) },
                        onLongPress = { bounds -> contextTarget = ListContextTarget(category, bounds) },
                        activeInMenu = contextTarget?.category?.id == category.id,
                    )
                }
            }
        }

        CreateCategoryButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navBottom + 18.dp),
            onClick = {
                if (categories.size >= CategoryRepository.MAX_CATEGORIES) {
                    CallActions.toast(context, s.maxCategories)
                } else {
                    onCreate()
                }
            }
        )

        contextTarget?.let { tgt ->
            CategoryContextMenuOverlay(
                target = CategoryContextTarget(tgt.category, tgt.bounds),
                topInsetPx = statusBarPx,
                bottomInsetPx = navBarPx,
                onEdit = { onEdit(tgt.category.id) },
                onDelete = { pendingDelete = tgt.category },
                onClosed = { contextTarget = null },
            )
        }
    }

    pendingDelete?.let { cat ->
        val n = cat.memberCount
        AppMessageDialog(
            onDismissRequest = { pendingDelete = null },
            title = s.deleteTitle,
            message = if (n > 0) s.deleteWithMembers(categoryLabel(cat), n) else s.deleteEmpty(categoryLabel(cat)),
            buttons = listOf(
                DialogButton(s.cancel, TextSecondary) { pendingDelete = null },
                DialogButton(s.deleteConfirm, AccentRed, bold = true) {
                    scope.launch { repo.deleteCategory(cat.id) }
                    pendingDelete = null
                },
            )
        )
    }
}

@Composable
private fun ListTopBar(count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appStrings().common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = appStrings().category.listTitle,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count/${CategoryRepository.MAX_CATEGORIES}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun ListHint() {
    Text(
        text = appStrings().category.settingsSubtitle,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
    )
}

@Composable
private fun CreateCategoryButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = AppBackground, modifier = Modifier.size(20.dp))
        Text(appStrings().category.createTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = AppBackground)
    }
}

/** Giữ [LayoutCoordinates] của thẻ ngoài snapshot state — đọc lúc nhấn giữ để lấy khung item hiện tại. */
private class CoordsHolder(var value: LayoutCoordinates? = null)

@Composable
private fun CategoryListItem(
    category: Category,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
    activeInMenu: Boolean,
) {
    val cardCoords = remember { CoordsHolder() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .onGloballyPositioned { cardCoords.value = it }
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f }
    ) {
        CategoryRowCard(
            category = category,
            onClick = onClick,
            onLongClick = {
                val bounds = cardCoords.value?.takeIf { it.isAttached }?.boundsInWindow()
                if (bounds != null) onLongPress(bounds)
            },
        )
    }
}

/**
 * Thẻ hiển thị một nhóm — DÙNG CHUNG cho danh sách và bản sao "nhấc lên" trong context-menu (như ListCallCard).
 * Không truyền [onClick] → thẻ tĩnh (bản sao overlay).
 */
@Composable
fun CategoryRowCard(
    category: Category,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val s = appStrings().category
    PanelCard(modifier = modifier.fillMaxWidth(), radius = 18.dp) {
        val interaction = remember { MutableInteractionSource() }
        val rowModifier = if (onClick != null) {
            Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = rememberPressHighlight(),
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
        } else {
            Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            CategoryIconDisc(category.iconKey, category.colorArgb, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryLabel(category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (category.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = s.memberCount(category.memberCount),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
