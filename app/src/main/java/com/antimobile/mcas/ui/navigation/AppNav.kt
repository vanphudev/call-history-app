package com.antimobile.mcas.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antimobile.mcas.data.agency.AgencyDataset
import com.antimobile.mcas.ui.agency.AgencyDirectoryScreen
import com.antimobile.mcas.ui.agency.AgencyDirectoryViewModel
import com.antimobile.mcas.ui.settings.LegalScreen
import com.antimobile.mcas.ui.settings.LegalViewModel
import com.antimobile.mcas.ui.calldetail.CallDetailScreen
import com.antimobile.mcas.ui.calldetail.CallDetailViewModel
import com.antimobile.mcas.ui.callhistory.AllCallsScreen
import com.antimobile.mcas.ui.timeline.TimelineScreen
import com.antimobile.mcas.ui.calllist.CallListViewModel
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.data.messaging.model.MessagingLaunch
import com.antimobile.mcas.ui.home.HomeScreen
import com.antimobile.mcas.ui.home.HomeTab
import com.antimobile.mcas.ui.messaging.ConversationListViewModel
import com.antimobile.mcas.ui.messaging.ConversationScreen
import com.antimobile.mcas.ui.messaging.ConversationViewModel
import com.antimobile.mcas.ui.messaging.NewMessageScreen
import com.antimobile.mcas.ui.messaging.NewMessageViewModel
import com.antimobile.mcas.ui.category.CategoryEditorScreen
import com.antimobile.mcas.ui.category.CategoryEditorViewModel
import com.antimobile.mcas.ui.category.CategoryListScreen
import com.antimobile.mcas.ui.blocking.CallBlockRuleEditorScreen
import com.antimobile.mcas.ui.blocking.CallBlockRuleEditorViewModel
import com.antimobile.mcas.ui.blocking.CallBlockScreen
import com.antimobile.mcas.ui.blocking.CallBlockSettingsScreen
import com.antimobile.mcas.ui.blocking.CallBlockNotificationAdvancedScreen
import com.antimobile.mcas.ui.blocking.CallBlockViewModel
import com.antimobile.mcas.ui.blocking.CallBlockNumberListScreen
import com.antimobile.mcas.ui.blocking.CallBlockGroupScreen
import com.antimobile.mcas.ui.blocking.CallBlockAdvancedRulesScreen
import com.antimobile.mcas.ui.blocking.CallBlockCommonIssuesScreen
import com.antimobile.mcas.data.blocking.CallBlockAction
import com.antimobile.mcas.ui.blocking.rememberCallBlockNotificationUiState
import com.antimobile.mcas.ui.components.UpdateNoticeModals
import com.antimobile.mcas.ui.contacts.ContactsScreen
import com.antimobile.mcas.ui.contacts.ContactsViewModel
import com.antimobile.mcas.ui.donate.DonateScreen
import com.antimobile.mcas.ui.donate.DonateViewModel
import com.antimobile.mcas.ui.coststats.CostStatsScreen
import com.antimobile.mcas.ui.coststats.CostStatsViewModel
import com.antimobile.mcas.ui.phonestats.PhoneStatsScreen
import com.antimobile.mcas.ui.phonestats.PhoneStatsViewModel
import com.antimobile.mcas.ui.repeatstats.RepeatStatsScreen
import com.antimobile.mcas.ui.repeatstats.RepeatStatsViewModel
import com.antimobile.mcas.ui.settings.BackupScreen
import com.antimobile.mcas.ui.settings.BackupViewModel
import com.antimobile.mcas.ui.settings.FontSizeScreen
import com.antimobile.mcas.ui.settings.LanguageScreen
import com.antimobile.mcas.ui.settings.MessageTemplateManagerScreen
import com.antimobile.mcas.ui.settings.MyNumberScreen
import com.antimobile.mcas.ui.settings.QrScanHistoryScreen
import com.antimobile.mcas.ui.settings.SettingsScreen
import com.antimobile.mcas.ui.settings.ThemeScreen
import com.antimobile.mcas.ui.outgoing.OutgoingCallSettingsScreen
import com.antimobile.mcas.ui.stats.DetailedStatsScreen
import com.antimobile.mcas.ui.stats.DetailedStatsViewModel
import com.antimobile.mcas.ui.components.AppToastType
import com.antimobile.mcas.util.CallActions

private object Routes {
    const val LIST = "list"
    const val DETAIL = "detail"
    const val ALL_CALLS = "allcalls"
    const val TIMELINE = "timeline"
    const val CONTACTS = "contacts"
    const val COST = "coststats"
    const val PHONE_STATS = "phonestats"
    const val STATS = "detailedstats"
    const val REPEAT = "repeatstats"
    const val SETTINGS = "settings"
    const val TEMPLATES = "templates"
    const val MY_NUMBER = "mynumber"
    const val QR_HISTORY = "qrhistory"
    const val FONT_SIZE = "fontsize"
    const val LANGUAGE = "language"
    const val THEME = "theme"
    const val OUTGOING_CALL_SETTINGS = "outgoing-call/settings"
    const val DIRECTORY = "directory/{dataset}"
    fun directory(datasetKey: String) = "directory/$datasetKey"
    const val LEGAL = "legal/{doc}"
    fun legal(doc: String) = "legal/$doc"
    const val CATEGORIES = "categories"
    const val CATEGORY_EDIT = "category/{id}"
    fun categoryEdit(id: String) = "category/$id"
    const val BACKUP = "backup"
    const val CALL_BLOCK = "callblock"
    const val CALL_BLOCK_SETTINGS = "callblock/settings"
    const val CALL_BLOCK_NOTIFICATION_ADVANCED = "callblock/settings/notifications"
    const val CALL_BLOCK_RULE = "callblock/rule/{id}"
    fun callBlockRule(id: String) = "callblock/rule/$id"
    const val CALL_BLOCK_ALLOWLIST = "callblock/allowlist"
    const val CALL_BLOCK_BLOCKLIST = "callblock/blocklist"
    const val CALL_BLOCK_GROUPS = "callblock/groups"
    const val CALL_BLOCK_ADVANCED = "callblock/advanced"
    const val CALL_BLOCK_COMMON_ISSUES = "callblock/common-issues"
    const val DONATE = "donate"
    const val MESSAGE_THREAD = "messages/thread"
    const val MESSAGE_NEW = "messages/new"
}

@Composable
fun AppNav(
    messagingLaunch: MessagingLaunch? = null,
    onMessagingLaunchConsumed: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val listVm: CallListViewModel = viewModel()
    val messagingVm: ConversationListViewModel = viewModel()
    val conversationVm: ConversationViewModel = viewModel()
    val newMessageVm: NewMessageViewModel = viewModel()
    val detailVm: CallDetailViewModel = viewModel()
    val costVm: CostStatsViewModel = viewModel()
    val phoneStatsVm: PhoneStatsViewModel = viewModel()
    val blockNotificationUiState = rememberCallBlockNotificationUiState()
    var homeTab by rememberSaveable { mutableStateOf(HomeTab.CALLS) }
    var messagingCapabilityEpoch by remember { mutableIntStateOf(0) }

    fun openConversation(
        threadId: Long,
        address: String,
        displayName: String? = null,
        photoUri: String? = null,
        body: String = "",
        subject: String = "",
    ) {
        conversationVm.openThread(threadId, address, displayName, photoUri, body, subject)
        nav.navigate(Routes.MESSAGE_THREAD) { launchSingleTop = true }
    }

    fun openAddress(address: String, body: String = "", subject: String = "") {
        conversationVm.openAddress(address, body, subject)
        nav.navigate(Routes.MESSAGE_THREAD) { launchSingleTop = true }
    }

    // Quay lại app từ nền (ON_START của Activity) → TỰ nạp lại dữ liệu, không cần bấm "Làm mới".
    // Chỉ làm mới danh sách khi đã từng nạp (bỏ qua lần khởi động lạnh — màn hình tự nạp lần đầu);
    // chi tiết & cước tự bỏ qua nếu chưa mở số nào.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (listVm.loaded) listVm.load()
        detailVm.refresh()
        costVm.refresh()
        phoneStatsVm.refresh()
        if (homeTab == HomeTab.MESSAGES) messagingVm.refreshCapability()
        messagingCapabilityEpoch++
    }

    LaunchedEffect(messagingLaunch?.nonce, messagingCapabilityEpoch) {
        val launch = messagingLaunch ?: return@LaunchedEffect
        homeTab = HomeTab.MESSAGES
        val consumed = when {
            launch.unsupportedMultipleRecipients -> {
                CallActions.toast(
                context,
                com.antimobile.mcas.i18n.appStrings().messaging.externalMultipleRecipients,
                AppToastType.Warning,
            )
                true
            }
            launch.unsupportedMmsPayload -> {
                CallActions.toast(
                context,
                com.antimobile.mcas.i18n.appStrings().messaging.externalMmsPayload,
                AppToastType.Warning,
            )
                true
            }
            !SmsRole.isHeld(context) -> false
            launch.threadId != null && !launch.recipient.isNullOrBlank() -> {
                openConversation(launch.threadId, launch.recipient, body = launch.body, subject = launch.subject)
                true
            }
            !launch.recipient.isNullOrBlank() -> {
                openAddress(launch.recipient, launch.body, launch.subject)
                true
            }
            else -> {
                newMessageVm.prepare(body = launch.body)
                nav.navigate(Routes.MESSAGE_NEW) { launchSingleTop = true }
                true
            }
        }
        if (consumed) onMessagingLaunchConsumed(launch.nonce)
    }

    NavHost(navController = nav, startDestination = Routes.LIST) {

        composable(
            route = Routes.LIST,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) }
        ) {
            HomeScreen(
                callListVm = listVm,
                messagingVm = messagingVm,
                selectedTab = homeTab,
                onSelectTab = { homeTab = it },
                onOpenConversation = { conversation ->
                    openConversation(
                        threadId = conversation.threadId,
                        address = conversation.address,
                        displayName = conversation.displayName,
                        photoUri = conversation.photoUri,
                    )
                },
                onNewMessage = {
                    newMessageVm.prepare()
                    nav.navigate(Routes.MESSAGE_NEW)
                },
                onOpenNumber = { number ->
                    detailVm.load(number)
                    nav.navigate(Routes.DETAIL)
                },
                onOpenContacts = { nav.navigate(Routes.CONTACTS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenCreateCategory = { nav.navigate(Routes.categoryEdit("new")) }
            )
        }

        composable(
            route = Routes.MESSAGE_THREAD,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            ConversationScreen(vm = conversationVm, onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.MESSAGE_NEW,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            NewMessageScreen(
                vm = newMessageVm,
                onBack = { nav.popBackStack() },
                onOpenRecipient = { address, body ->
                    conversationVm.openAddress(address, body)
                    nav.navigate(Routes.MESSAGE_THREAD) {
                        popUpTo(Routes.MESSAGE_NEW) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.DETAIL,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            CallDetailScreen(
                vm = detailVm,
                onBack = { nav.popBackStack() },
                onSeeAll = { nav.navigate(Routes.ALL_CALLS) },
                onCostStats = {
                    detailVm.detail?.number?.let { number ->
                        costVm.load(number)
                        nav.navigate(Routes.COST)
                    }
                },
                onOpenStats = {
                    detailVm.detail?.number?.let { number ->
                        phoneStatsVm.load(number)
                        nav.navigate(Routes.PHONE_STATS)
                    }
                },
                onCreateCategory = { nav.navigate(Routes.categoryEdit("new")) }
            )
        }

        composable(
            route = Routes.ALL_CALLS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            AllCallsScreen(
                vm = detailVm,
                onBack = { nav.popBackStack() },
                onOpenTimeline = { nav.navigate(Routes.TIMELINE) },
                onOpenStats = {
                    detailVm.detail?.number?.let { number ->
                        phoneStatsVm.load(number)
                        nav.navigate(Routes.PHONE_STATS)
                    }
                }
            )
        }

        composable(
            route = Routes.TIMELINE,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            TimelineScreen(
                vm = detailVm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.CONTACTS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            val contactsVm: ContactsViewModel = viewModel()
            ContactsScreen(
                vm = contactsVm,
                onBack = { nav.popBackStack() },
                onOpenNumber = { number ->
                    detailVm.load(number)
                    nav.navigate(Routes.DETAIL)
                },
                onCreateCategory = { nav.navigate(Routes.categoryEdit("new")) }
            )
        }

        composable(
            route = Routes.COST,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            CostStatsScreen(
                vm = costVm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.PHONE_STATS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            PhoneStatsScreen(
                vm = phoneStatsVm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.SETTINGS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            // Chỉ điều hướng khi màn Cài đặt còn RESUMED → chặn double-tap nhanh đẩy 2 màn trùng nhau.
            val whenResumed: (() -> Unit) -> Unit = { action ->
                if (nav.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
                    action()
                }
            }
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenStats = { whenResumed { nav.navigate(Routes.STATS) } },
                onOpenRepeatStats = { whenResumed { nav.navigate(Routes.REPEAT) } },
                onOpenTemplates = { whenResumed { nav.navigate(Routes.TEMPLATES) } },
                onOpenMyNumber = { whenResumed { nav.navigate(Routes.MY_NUMBER) } },
                onOpenQrHistory = { whenResumed { nav.navigate(Routes.QR_HISTORY) } },
                onOpenFontSize = { whenResumed { nav.navigate(Routes.FONT_SIZE) } },
                onOpenLanguage = { whenResumed { nav.navigate(Routes.LANGUAGE) } },
                onOpenTheme = { whenResumed { nav.navigate(Routes.THEME) } },
                onOpenDirectory = { dataset -> whenResumed { nav.navigate(Routes.directory(dataset.key)) } },
                onOpenLegal = { doc -> whenResumed { nav.navigate(Routes.legal(doc)) } },
                onOpenCategories = { whenResumed { nav.navigate(Routes.CATEGORIES) } },
                onOpenCallBlocking = { whenResumed { nav.navigate(Routes.CALL_BLOCK) } },
                onOpenOutgoingCallSettings = {
                    whenResumed { nav.navigate(Routes.OUTGOING_CALL_SETTINGS) }
                },
                onOpenBackup = { whenResumed { nav.navigate(Routes.BACKUP) } },
                onOpenDonate = { whenResumed { nav.navigate(Routes.DONATE) } }
            )
        }

        composable(
            route = Routes.TEMPLATES,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            MessageTemplateManagerScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.MY_NUMBER,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            MyNumberScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.QR_HISTORY,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            QrScanHistoryScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.FONT_SIZE,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            FontSizeScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.LANGUAGE,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            LanguageScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.THEME,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            ThemeScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.OUTGOING_CALL_SETTINGS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            OutgoingCallSettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.STATS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            val statsVm: DetailedStatsViewModel = viewModel()
            DetailedStatsScreen(
                vm = statsVm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.REPEAT,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            val repeatVm: RepeatStatsViewModel = viewModel()
            RepeatStatsScreen(
                vm = repeatVm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.DIRECTORY,
            arguments = listOf(navArgument("dataset") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { backStackEntry ->
            val dataset = AgencyDataset.fromKey(backStackEntry.arguments?.getString("dataset"))
            if (dataset == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                val agencyVm: AgencyDirectoryViewModel = viewModel()
                AgencyDirectoryScreen(
                    dataset = dataset,
                    vm = agencyVm,
                    onBack = { nav.popBackStack() }
                )
            }
        }

        composable(
            route = Routes.LEGAL,
            arguments = listOf(navArgument("doc") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { backStackEntry ->
            val legalVm: LegalViewModel = viewModel()
            LegalScreen(
                doc = backStackEntry.arguments?.getString("doc").orEmpty(),
                vm = legalVm,
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.CATEGORIES,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            CategoryListScreen(
                onBack = { nav.popBackStack() },
                onCreate = { nav.navigate(Routes.categoryEdit("new")) },
                onEdit = { id -> nav.navigate(Routes.categoryEdit(id.toString())) }
            )
        }

        composable(
            route = Routes.CATEGORY_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { backStackEntry ->
            // id = "new" (tạo mới) → 0L; số → sửa nhóm đó.
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            val editorVm: CategoryEditorViewModel = viewModel()
            CategoryEditorScreen(
                vm = editorVm,
                categoryId = id,
                onExit = { nav.popBackStack() },
                onOpenNumber = { number ->
                    detailVm.load(number)
                    nav.navigate(Routes.DETAIL)
                }
            )
        }

        composable(
            route = Routes.BACKUP,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            val backupVm: BackupViewModel = viewModel()
            BackupScreen(vm = backupVm, onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.CALL_BLOCK,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { backStackEntry ->
            val blockVm: CallBlockViewModel = viewModel()
            val whenResumed: (() -> Unit) -> Unit = { action ->
                if (backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED) action()
            }
            CallBlockScreen(
                vm = blockVm,
                notificationUiState = blockNotificationUiState,
                onBack = { nav.popBackStack() },
                onOpenSettings = {
                    whenResumed {
                        nav.navigate(Routes.CALL_BLOCK_SETTINGS) { launchSingleTop = true }
                    }
                },
                onOpenAllowlist = { whenResumed { nav.navigate(Routes.CALL_BLOCK_ALLOWLIST) { launchSingleTop = true } } },
                onOpenBlocklist = { whenResumed { nav.navigate(Routes.CALL_BLOCK_BLOCKLIST) { launchSingleTop = true } } },
                onOpenGroups = { whenResumed { nav.navigate(Routes.CALL_BLOCK_GROUPS) { launchSingleTop = true } } },
                onOpenAdvancedRules = { whenResumed { nav.navigate(Routes.CALL_BLOCK_ADVANCED) { launchSingleTop = true } } },
                onOpenCommonIssues = { whenResumed { nav.navigate(Routes.CALL_BLOCK_COMMON_ISSUES) { launchSingleTop = true } } },
                onOpenNumber = { number ->
                    whenResumed {
                        detailVm.load(number)
                        nav.navigate(Routes.DETAIL) { launchSingleTop = true }
                    }
                },
            )
        }

        composable(
            route = Routes.CALL_BLOCK_ALLOWLIST,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            val blockVm: CallBlockViewModel = viewModel()
            val contactVm: CallBlockRuleEditorViewModel = viewModel()
            CallBlockNumberListScreen(
                vm = blockVm,
                contactVm = contactVm,
                callListVm = listVm,
                action = CallBlockAction.ALLOW,
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            route = Routes.CALL_BLOCK_BLOCKLIST,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            val blockVm: CallBlockViewModel = viewModel()
            val contactVm: CallBlockRuleEditorViewModel = viewModel()
            CallBlockNumberListScreen(
                vm = blockVm,
                contactVm = contactVm,
                callListVm = listVm,
                action = CallBlockAction.BLOCK,
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            route = Routes.CALL_BLOCK_GROUPS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            val blockVm: CallBlockViewModel = viewModel()
            CallBlockGroupScreen(vm = blockVm, onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.CALL_BLOCK_ADVANCED,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) { backStackEntry ->
            val blockVm: CallBlockViewModel = viewModel()
            val whenResumed: (() -> Unit) -> Unit = { action ->
                if (backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED) action()
            }
            CallBlockAdvancedRulesScreen(
                vm = blockVm,
                onBack = { nav.popBackStack() },
                onCreate = { whenResumed { nav.navigate(Routes.callBlockRule("new")) } },
                onEdit = { id -> whenResumed { nav.navigate(Routes.callBlockRule(id.toString())) } },
            )
        }

        composable(
            route = Routes.CALL_BLOCK_COMMON_ISSUES,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            CallBlockCommonIssuesScreen(
                onBack = { nav.popBackStack() },
                onOpenBlockSettings = {
                    nav.navigate(Routes.CALL_BLOCK_SETTINGS) { launchSingleTop = true }
                },
            )
        }

        composable(
            route = Routes.CALL_BLOCK_SETTINGS,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            CallBlockSettingsScreen(
                notificationUiState = blockNotificationUiState,
                onBack = { nav.popBackStack() },
                onOpenAdvancedNotifications = {
                    nav.navigate(Routes.CALL_BLOCK_NOTIFICATION_ADVANCED) { launchSingleTop = true }
                },
            )
        }

        composable(
            route = Routes.CALL_BLOCK_NOTIFICATION_ADVANCED,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) },
        ) {
            CallBlockNotificationAdvancedScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.CALL_BLOCK_RULE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("id")
            val ruleId = rawId?.takeIf { it != "new" }?.toLongOrNull()
            if (rawId == "new" || ruleId != null) {
                val editorVm: CallBlockRuleEditorViewModel = viewModel()
                CallBlockRuleEditorScreen(
                    vm = editorVm,
                    callListVm = listVm,
                    ruleId = ruleId,
                    onExit = { nav.popBackStack() },
                )
            } else {
                LaunchedEffect(Unit) { nav.popBackStack() }
            }
        }

        composable(
            route = Routes.DONATE,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            val donateVm: DonateViewModel = viewModel()
            DonateScreen(vm = donateVm, onBack = { nav.popBackStack() })
        }
    }

    // Thông báo cập nhật (đổi chính sách / tính năng mới) — dialog là cửa sổ riêng nên đặt cạnh NavHost.
    // Đặt ở đây vì nút "Xem thêm" cần `nav`, và AppNav chỉ được dựng SAU khi qua cổng quyền + điều khoản.
    UpdateNoticeModals(onOpenSettings = { nav.navigate(Routes.SETTINGS) })
}
