package com.antimobile.mcas.ui.category

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
// Danh mục icon lớn (~70 icon) → dùng wildcard cho gọn. R8 vẫn tree-shake theo THAM CHIẾU (mỗi Icons.Filled.X
// là 1 val lười, chỉ cái được tham chiếu mới được giữ), nên wildcard KHÔNG làm phình APK.
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CancelPresentation
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.antimobile.mcas.R
import com.antimobile.mcas.data.local.Category
import com.antimobile.mcas.data.local.CategoryRepository
import com.antimobile.mcas.i18n.appStrings

/**
 * Biểu tượng nhóm có 2 DẠNG: icon material (vector, tô màu người dùng chọn) hoặc LOGO app (ảnh drawable, giữ
 * nguyên màu thương hiệu). [key] lưu vào DB; logo dùng tiền tố "app:" (vd "app:grab").
 */
sealed interface CategoryGlyph {
    data class Vec(val icon: ImageVector) : CategoryGlyph
    data class Res(@param:DrawableRes val id: Int) : CategoryGlyph
}

/** Một lựa chọn icon: [key] ổn định (lưu DB) + [glyph]. */
data class IconChoice(val key: String, val glyph: CategoryGlyph)

/** Một NHÓM icon = 1 TRANG trong picker. [id] để lấy tiêu đề i18n qua [iconGroupTitle]. */
data class IconGroup(val id: String, val choices: List<IconChoice>)

private fun vec(key: String, icon: ImageVector) = IconChoice(key, CategoryGlyph.Vec(icon))
private fun logo(key: String, @DrawableRes res: Int) = IconChoice("app:$key", CategoryGlyph.Res(res))

/** LOGO các app/nền tảng giao hàng (ảnh trong res/drawable). */
private val DELIVERY_APPS: List<IconChoice> = listOf(
    logo("grab", R.drawable.ic_grab),
    logo("shopeefood", R.drawable.ic_shopeefood),
    logo("be", R.drawable.ic_be),
    logo("ahamove", R.drawable.ic_ahamove),
    logo("lalamove", R.drawable.ic_lalamove),
    logo("xanh_sm", R.drawable.ic_xanh_sm),
    logo("shopee_express", R.drawable.ic_shopee_express),
    logo("viettel_post", R.drawable.ic_viettel_post),
    logo("tiktok_express", R.drawable.ic_tiktok_express),
    logo("ghn", R.drawable.ic_ghn),
    logo("ghtk", R.drawable.ic_ghtk),
)

/**
 * Các NHÓM icon (mỗi nhóm 1 trang picker). QUAN TRỌNG: [key] phải DUY NHẤT trên toàn bộ các nhóm.
 * Nhóm mặc định của app dùng key "work"/"favorite" — nằm trong nhóm work/basic bên dưới.
 */
val ICON_GROUPS: List<IconGroup> = listOf(
    IconGroup(
        "basic", listOf(
            vec("favorite", Icons.Filled.Favorite),
            vec("star", Icons.Filled.Star),
            vec("home", Icons.Filled.Home),
            vec("person", Icons.Filled.Person),
            vec("groups", Icons.Filled.Groups),
            vec("family", Icons.Filled.FamilyRestroom),
            vec("school", Icons.Filled.School),
            vec("pets", Icons.Filled.Pets),
            vec("music", Icons.Filled.MusicNote),
            vec("soccer", Icons.Filled.SportsSoccer),
            vec("flight", Icons.Filled.Flight),
            vec("car", Icons.Filled.DirectionsCar),
            vec("restaurant", Icons.Filled.Restaurant),
            vec("cafe", Icons.Filled.LocalCafe),
            vec("cart", Icons.Filled.ShoppingCart),
            vec("bag", Icons.Filled.ShoppingBag),
            vec("hospital", Icons.Filled.LocalHospital),
            vec("celebration", Icons.Filled.Celebration),
        )
    ),
    IconGroup("delivery", DELIVERY_APPS),
    IconGroup(
        "work", listOf(
            vec("work", Icons.Filled.Work),
            vec("business", Icons.Filled.Business),
            vec("business_center", Icons.Filled.BusinessCenter),
            vec("badge", Icons.Filled.Badge),
            vec("handshake", Icons.Filled.Handshake),
            vec("meeting", Icons.Filled.MeetingRoom),
            vec("support_agent", Icons.Filled.SupportAgent),
            vec("engineering", Icons.Filled.Engineering),
            vec("store", Icons.Filled.Store),
            vec("storefront", Icons.Filled.Storefront),
            vec("bank", Icons.Filled.AccountBalance),
            vec("receipt", Icons.Filled.ReceiptLong),
            vec("description", Icons.Filled.Description),
            vec("calendar", Icons.Filled.CalendarMonth),
            vec("task", Icons.Filled.Task),
            vec("pos", Icons.Filled.PointOfSale),
        )
    ),
    IconGroup(
        "issue", listOf(
            vec("remove_cart", Icons.Filled.RemoveShoppingCart),
            vec("cancel_presentation", Icons.Filled.CancelPresentation),
            vec("report_problem", Icons.Filled.ReportProblem),
            vec("money_off", Icons.Filled.MoneyOff),
            vec("block", Icons.Filled.Block),
            vec("do_not_disturb", Icons.Filled.DoNotDisturbOn),
            vec("thumb_down", Icons.Filled.ThumbDown),
            vec("sad", Icons.Filled.SentimentVeryDissatisfied),
            vec("warning", Icons.Filled.Warning),
            vec("assignment_late", Icons.Filled.AssignmentLate),
            vec("report", Icons.Filled.Report),
            vec("priority_high", Icons.Filled.PriorityHigh),
            vec("cancel", Icons.Filled.Cancel),
            vec("undo", Icons.Filled.Undo),
            vec("refund", Icons.Filled.CurrencyExchange),
            vec("dangerous", Icons.Filled.Dangerous),
        )
    ),
    IconGroup(
        "social", listOf(
            vec("people", Icons.Filled.People),
            vec("emoji_people", Icons.Filled.EmojiPeople),
            vec("volunteer", Icons.Filled.VolunteerActivism),
            vec("happy", Icons.Filled.SentimentSatisfied),
            vec("thumb_up", Icons.Filled.ThumbUp),
            vec("emoji", Icons.Filled.EmojiEmotions),
            vec("mood", Icons.Filled.Mood),
            vec("nightlife", Icons.Filled.Nightlife),
            vec("bar", Icons.Filled.LocalBar),
            vec("liquor", Icons.Filled.Liquor),
            vec("coffee", Icons.Filled.Coffee),
            vec("fastfood", Icons.Filled.Fastfood),
        )
    ),
)

private val GLYPH_BY_KEY: Map<String, CategoryGlyph> =
    ICON_GROUPS.flatMap { it.choices }.associate { it.key to it.glyph }

/** Glyph theo key đã lưu; fallback [Icons.Filled.Category] nếu key lạ. */
fun categoryGlyph(key: String): CategoryGlyph = GLYPH_BY_KEY[key] ?: CategoryGlyph.Vec(Icons.Filled.Category)

/** key có phải logo (ảnh) không — dùng để bỏ qua màu khi vẽ. */
fun isLogoKey(key: String): Boolean = categoryGlyph(key) is CategoryGlyph.Res

/** Danh sách key icon MATERIAL (có tô màu) — để random mặc định lúc tạo nhóm. */
val MATERIAL_ICON_KEYS: List<String> =
    ICON_GROUPS.flatMap { it.choices }.filter { it.glyph is CategoryGlyph.Vec }.map { it.key }

/** Tiêu đề nhóm icon theo i18n. */
fun iconGroupTitle(id: String): String = appStrings().category.let {
    when (id) {
        "basic" -> it.iconGroupBasic
        "delivery" -> it.iconGroupDelivery
        "work" -> it.iconGroupWork
        "issue" -> it.iconGroupIssue
        "social" -> it.iconGroupSocial
        else -> id
    }
}

/**
 * Tên hiển thị của nhóm: nhóm mặc định lấy từ i18n (dịch theo ngôn ngữ), nhóm người dùng giữ nguyên tên.
 */
fun categoryLabel(category: Category): String = when (category.builtInKey) {
    CategoryRepository.BUILTIN_WORK -> appStrings().category.builtinWork
    CategoryRepository.BUILTIN_FAVORITE -> appStrings().category.builtinFavorite
    else -> category.name
}

/**
 * 12 màu accent độ sáng trung tính — đọc tốt (≥3:1) trên cả nền thẻ sáng lẫn tối, dùng chung 1 hex cho cả 2
 * theme. Lưu dạng ARGB Long. (Chỉ áp cho icon material; logo app giữ màu ảnh gốc.)
 */
val CATEGORY_COLORS: List<Long> = listOf(
    0xFFE5484DL, // red
    0xFFE8710AL, // orange
    0xFFC99213L, // amber
    0xFF2F9E5BL, // green
    0xFF12A594L, // teal
    0xFF1BA3C4L, // cyan
    0xFF2E86E0L, // blue
    0xFF5B67E0L, // indigo
    0xFF8A5CF0L, // violet
    0xFFE0559AL, // pink
    0xFF96684AL, // brown
    0xFF6E7C8AL, // slate
)

/** Màu placeholder lưu cùng nhóm khi chọn LOGO app (không dùng để vẽ glyph — chỉ để không rỗng). */
const val LOGO_PLACEHOLDER_COLOR: Long = 0xFFFFFFFFL
