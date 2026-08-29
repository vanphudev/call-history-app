package com.antimobile.mcas.ui.home

import androidx.compose.runtime.Composable
import com.antimobile.mcas.data.messaging.model.ConversationSummary
import com.antimobile.mcas.ui.calllist.CallListScreen
import com.antimobile.mcas.ui.calllist.CallListViewModel
import com.antimobile.mcas.ui.messaging.ConversationListViewModel

/** Khung màn chính dùng chung cho hai miền Cuộc gọi và Nhắn tin. */
@Composable
fun HomeScreen(
    callListVm: CallListViewModel,
    messagingVm: ConversationListViewModel,
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onOpenConversation: (ConversationSummary) -> Unit,
    onNewMessage: () -> Unit,
    onOpenNumber: (String) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreateCategory: () -> Unit,
) {
    CallListScreen(
        vm = callListVm,
        messagingVm = messagingVm,
        homeTab = selectedTab,
        onSelectHomeTab = onSelectTab,
        onOpenConversation = onOpenConversation,
        onNewMessage = onNewMessage,
        onOpenNumber = onOpenNumber,
        onOpenContacts = onOpenContacts,
        onOpenSettings = onOpenSettings,
        onOpenCreateCategory = onOpenCreateCategory,
    )
}
