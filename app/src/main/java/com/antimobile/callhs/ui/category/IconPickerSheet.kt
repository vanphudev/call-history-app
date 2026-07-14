package com.antimobile.callhs.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.theme.ChipBg
import com.antimobile.callhs.ui.theme.ChipSelectedBg
import com.antimobile.callhs.ui.theme.ChipSelectedText
import com.antimobile.callhs.ui.theme.ChipText
import kotlinx.coroutines.launch

/** Số cột lưới icon. */
private const val ICON_COLUMNS = 5

/**
 * CHIỀU CAO CỐ ĐỊNH của vùng pager — CỐ Ý cố định để khi vuốt sang nhóm ÍT icon hay NHIỀU icon thì khung
 * KHÔNG bị nhảy lên/xuống. Nhóm nào nhiều hơn thì tự CUỘN dọc bên trong (grid) trong khung cố định này.
 */
private val PICKER_PAGER_HEIGHT = 300.dp

/**
 * Bottom sheet chọn BIỂU TƯỢNG — dạng PHÂN TRANG (pager): mỗi nhóm icon (Cơ bản / Giao hàng / Công việc /
 * Sự cố giao / Xã giao) là 1 TRANG. Icon material được gán MÀU NGẪU NHIÊN mỗi lần mở; logo app giữ màu gốc.
 */
@Composable
fun IconPickerSheet(
    onDismiss: () -> Unit,
    onPick: (iconKey: String, colorArgb: Long) -> Unit,
) {
    // random màu cho từng icon material, cố định trong phiên mở sheet.
    val colorByKey = remember { MATERIAL_ICON_KEYS.associateWith { CATEGORY_COLORS.random() } }
    val pagerState = rememberPagerState(pageCount = { ICON_GROUPS.size })
    val scope = rememberCoroutineScope()

    AppBottomSheet(onDismiss = onDismiss, title = appStrings().category.pickIconTitle) { close ->
        GroupTabRow(
            groups = ICON_GROUPS,
            selected = pagerState.currentPage,
            onSelect = { i -> scope.launch { pagerState.animateScrollToPage(i) } },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(PICKER_PAGER_HEIGHT),
            pageSpacing = 8.dp,
        ) { page ->
            IconGrid(
                group = ICON_GROUPS[page],
                colorByKey = colorByKey,
                onPick = { key, argb -> onPick(key, argb); close() },
            )
        }
    }
}

@Composable
private fun GroupTabRow(groups: List<IconGroup>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEachIndexed { i, g ->
            val sel = i == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sel) ChipSelectedBg else ChipBg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = iconGroupTitle(g.id),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (sel) ChipSelectedText else ChipText,
                )
            }
        }
    }
}

@Composable
private fun IconGrid(group: IconGroup, colorByKey: Map<String, Long>, onPick: (String, Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(ICON_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(group.choices, key = { it.key }) { choice ->
            when (val g = choice.glyph) {
                is CategoryGlyph.Res -> LogoCell(g.id) { onPick(choice.key, LOGO_PLACEHOLDER_COLOR) }
                is CategoryGlyph.Vec -> {
                    val argb = colorByKey[choice.key] ?: CATEGORY_COLORS.first()
                    VecCell(g.icon, Color(argb)) { onPick(choice.key, argb) }
                }
            }
        }
    }
}

@Composable
private fun VecCell(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LogoCell(resId: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
