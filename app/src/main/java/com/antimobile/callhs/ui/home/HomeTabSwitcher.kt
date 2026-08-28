package com.antimobile.callhs.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.TabSelectedBg
import com.antimobile.callhs.ui.theme.TabSelectedText
import com.antimobile.callhs.ui.theme.TabText

@Composable
fun HomeTabSwitcher(selected: HomeTab, onSelect: (HomeTab) -> Unit, modifier: Modifier = Modifier) {
    val s = appStrings().messaging
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(FieldSurface)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeTab.entries.forEach { tab ->
            val active = tab == selected
            val background by animateColorAsState(
                if (active) TabSelectedBg else androidx.compose.ui.graphics.Color.Transparent,
                tween(220),
                label = "home-tab-bg",
            )
            val foreground by animateColorAsState(
                if (active) TabSelectedText else TabText,
                tween(220),
                label = "home-tab-text",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(background)
                    .semantics { this.selected = active }
                    .clickable(role = Role.Tab) { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (tab == HomeTab.CALLS) s.callsTab else s.messagesTab,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 3.dp),
                )
            }
        }
    }
}
