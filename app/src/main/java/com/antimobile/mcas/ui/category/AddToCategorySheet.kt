package com.antimobile.mcas.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.local.Category
import com.antimobile.mcas.data.local.CategoryCatalog
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.PhoneKey

/**
 * Sheet "Thêm vào nhóm" cho một [number]: liệt kê các nhóm hiện có + tick nhóm mà số đang thuộc; chạm để
 * bật/tắt (host thực thi ghi DB + kiểm cap, danh sách tự cập nhật qua [CategoryCatalog]). Kèm mục "Tạo nhóm mới".
 */
@Composable
fun AddToCategorySheet(
    number: String,
    onDismiss: () -> Unit,
    onToggle: (Category, Boolean) -> Unit,
    onCreateNew: () -> Unit,
) {
    val s = appStrings().category
    val categories = CategoryCatalog.categories
    val key = PhoneKey.of(number)
    AppBottomSheet(onDismiss = onDismiss, title = s.addToCategoryTitle) { close ->
        categories.forEach { cat ->
            val isMember = key in CategoryCatalog.membersOf(cat.id)
            ToggleRow(cat, isMember) { onToggle(cat, !isMember) }
        }
        CreateNewRow(label = s.createNew) {
            onCreateNew()
            close()
        }
    }
}

@Composable
private fun ToggleRow(category: Category, isMember: Boolean, onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), radius = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconDisc(category.iconKey, category.colorArgb, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                text = categoryLabel(category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            if (isMember) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun CreateNewRow(label: String, onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), radius = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(CardFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Primary,
            )
        }
    }
}
