package com.antimobile.callhs.ui.blocking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antimobile.callhs.data.blocking.CallBlockCategorySelection
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.local.Category
import com.antimobile.callhs.data.local.CategoryCatalog
import com.antimobile.callhs.data.local.CategoryMember
import com.antimobile.callhs.data.local.CategoryRepository
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.category.CategoryRowCard
import com.antimobile.callhs.ui.category.categoryLabel
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.PhoneKey
import com.antimobile.callhs.util.formatPhone

/**
 * Two-level picker for copying exact numbers out of user-managed categories. Selections are keyed
 * by normalized phone number, so a number that belongs to several categories is only added once.
 */
@Composable
internal fun CallBlockCategoryPickerScreen(
    onBack: () -> Unit,
    onDone: (List<CallBlockCategorySelection>) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { CategoryRepository(context.applicationContext) }
    val categories = CategoryCatalog.categories
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedByKey by remember {
        mutableStateOf<Map<String, CallBlockCategorySelection>>(emptyMap())
    }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val s = appStrings().blocker

    LaunchedEffect(selectedCategoryId, categories) {
        if (selectedCategoryId != null && selectedCategory == null) selectedCategoryId = null
    }

    val navigateBack = {
        if (selectedCategoryId != null) selectedCategoryId = null else onBack()
    }
    BackHandler(onBack = navigateBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EditorTopBar(
                title = selectedCategory?.let(::categoryLabel) ?: s.sourceFromCategories,
                onBack = navigateBack,
            )
        },
        bottomBar = {
            EditorSaveBar(
                label = s.contactPickerDone,
                enabled = selectedByKey.isNotEmpty(),
                onSave = { onDone(selectedByKey.values.toList()) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedByKey.isNotEmpty()) {
                Text(
                    text = s.callHistoryPickerSelectedCount(selectedByKey.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp),
                )
            }

            if (selectedCategory == null) {
                CategoryList(
                    categories = categories,
                    onOpen = { selectedCategoryId = it.id },
                )
            } else {
                CategoryMemberList(
                    category = selectedCategory,
                    repo = repo,
                    selectedKeys = selectedByKey.keys,
                    onToggle = { member ->
                        val key = PhoneKey.of(member.rawNumber)
                        selectedByKey = if (key in selectedByKey) {
                            selectedByKey - key
                        } else {
                            selectedByKey + (
                                key to CallBlockCategorySelection(
                                    displayName = categoryLabel(selectedCategory),
                                    rawNumber = member.rawNumber,
                                )
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    onOpen: (Category) -> Unit,
) {
    if (categories.isEmpty()) {
        CategoryPickerMessage(appStrings().category.noCategories)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 18.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryRowCard(
                category = category,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                onClick = { onOpen(category) },
            )
        }
    }
}

@Composable
private fun CategoryMemberList(
    category: Category,
    repo: CategoryRepository,
    selectedKeys: Set<String>,
    onToggle: (CategoryMember) -> Unit,
) {
    val members by remember(repo, category.id) {
        repo.observeMembers(category.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectable = remember(members) {
        members.filter { member ->
            CallHistoryRuleCodec.isSelectableNumber(member.rawNumber) &&
                PhoneKey.of(member.rawNumber).length >= 3
        }
    }

    if (selectable.isEmpty()) {
        CategoryPickerMessage(appStrings().category.emptyMembers)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 18.dp),
    ) {
        items(
            items = selectable,
            key = { member -> PhoneKey.of(member.rawNumber) },
        ) { member ->
            val key = PhoneKey.of(member.rawNumber)
            CategoryMemberRow(
                member = member,
                selected = key in selectedKeys,
                onClick = { onToggle(member) },
            )
        }
    }
}

@Composable
private fun CategoryMemberRow(
    member: CategoryMember,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        radius = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(CardFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatPhone(member.rawNumber),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Primary else CardFill),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = appStrings().callList.selected,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(86.dp).clip(CircleShape).background(CardFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Category,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.65f),
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
