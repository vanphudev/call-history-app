package com.antimobile.mcas.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.Segmented

@Composable
fun HomeTabSwitcher(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val s = appStrings().messaging
    val tabs = HomeTab.entries
    val labels = tabs.map { tab ->
        when (tab) {
            HomeTab.CALLS -> s.callsTab
            HomeTab.MESSAGES -> s.messagesTab
        }
    }
    Segmented(
        labels = labels,
        semanticLabels = labels,
        selected = tabs.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> tabs.getOrNull(index)?.let(onSelect) },
        modifier = modifier,
    )
}
