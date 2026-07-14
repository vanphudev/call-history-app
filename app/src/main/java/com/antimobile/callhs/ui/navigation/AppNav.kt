package com.antimobile.callhs.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antimobile.callhs.data.agency.AgencyDataset
import com.antimobile.callhs.ui.agency.AgencyDirectoryScreen
import com.antimobile.callhs.ui.agency.AgencyDirectoryViewModel
import com.antimobile.callhs.ui.settings.LegalScreen
import com.antimobile.callhs.ui.settings.LegalViewModel
import com.antimobile.callhs.ui.calldetail.CallDetailScreen
import com.antimobile.callhs.ui.calldetail.CallDetailViewModel
import com.antimobile.callhs.ui.callhistory.AllCallsScreen
import com.antimobile.callhs.ui.timeline.TimelineScreen
import com.antimobile.callhs.ui.calllist.CallListScreen
import com.antimobile.callhs.ui.calllist.CallListViewModel
import com.antimobile.callhs.ui.category.CategoryEditorScreen
import com.antimobile.callhs.ui.category.CategoryEditorViewModel
import com.antimobile.callhs.ui.category.CategoryListScreen
import com.antimobile.callhs.ui.components.UpdateNoticeModals
import com.antimobile.callhs.ui.contacts.ContactsScreen
import com.antimobile.callhs.ui.contacts.ContactsViewModel
import com.antimobile.callhs.ui.donate.DonateScreen
import com.antimobile.callhs.ui.donate.DonateViewModel
import com.antimobile.callhs.ui.coststats.CostStatsScreen
import com.antimobile.callhs.ui.coststats.CostStatsViewModel
import com.antimobile.callhs.ui.phonestats.PhoneStatsScreen
import com.antimobile.callhs.ui.phonestats.PhoneStatsViewModel
import com.antimobile.callhs.ui.repeatstats.RepeatStatsScreen
import com.antimobile.callhs.ui.repeatstats.RepeatStatsViewModel
import com.antimobile.callhs.ui.settings.FontSizeScreen
import com.antimobile.callhs.ui.settings.LanguageScreen
import com.antimobile.callhs.ui.settings.MessageTemplateManagerScreen
import com.antimobile.callhs.ui.settings.MyNumberScreen
import com.antimobile.callhs.ui.settings.QrScanHistoryScreen
import com.antimobile.callhs.ui.settings.SettingsScreen
import com.antimobile.callhs.ui.settings.ThemeScreen
import com.antimobile.callhs.ui.stats.DetailedStatsScreen
import com.antimobile.callhs.ui.stats.DetailedStatsViewModel

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
    const val DIRECTORY = "directory/{dataset}"
    fun directory(datasetKey: String) = "directory/$datasetKey"
    const val LEGAL = "legal/{doc}"
    fun legal(doc: String) = "legal/$doc"
    const val CATEGORIES = "categories"
    const val CATEGORY_EDIT = "category/{id}"
    fun categoryEdit(id: String) = "category/$id"
    const val DONATE = "donate"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val listVm: CallListViewModel = viewModel()
    val detailVm: CallDetailViewModel = viewModel()
    val costVm: CostStatsViewModel = viewModel()
    val phoneStatsVm: PhoneStatsViewModel = viewModel()

    // Quay lại app từ nền (ON_START của Activity) → TỰ nạp lại dữ liệu, không cần bấm "Làm mới".
    // Chỉ làm mới danh sách khi đã từng nạp (bỏ qua lần khởi động lạnh — màn hình tự nạp lần đầu);
    // chi tiết & cước tự bỏ qua nếu chưa mở số nào.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (listVm.loaded) listVm.load()
        detailVm.refresh()
        costVm.refresh()
        phoneStatsVm.refresh()
    }

    NavHost(navController = nav, startDestination = Routes.LIST) {

        composable(
            route = Routes.LIST,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) }
        ) {
            CallListScreen(
                vm = listVm,
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
