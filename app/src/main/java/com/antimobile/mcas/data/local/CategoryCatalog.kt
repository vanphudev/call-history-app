package com.antimobile.mcas.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.antimobile.mcas.util.PhoneKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Nguồn phản ứng TOÀN APP cho nhóm phân loại — theo đúng idiom [com.antimobile.mcas.ui.theme.ThemeSettings]
 * / [com.antimobile.mcas.i18n.LanguageSettings] / [com.antimobile.mcas.util.ContactPhotoSignal]: state
 * Compose ([mutableStateOf]) đọc ở bất kỳ composable nào → badge trên avatar & chip lọc cập nhật TỨC THÌ
 * khi membership đổi, không phải nhồi dữ liệu qua từng ViewModel.
 *
 * [start] gọi 1 lần trong MainActivity.onCreate: seed 2 nhóm mặc định + mở collector app-lifetime.
 */
object CategoryCatalog {

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set

    var badges by mutableStateOf<Map<String, List<CategoryBadge>>>(emptyMap())
        private set

    var membersByCategory by mutableStateOf<Map<Long, Set<String>>>(emptyMap())
        private set

    private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start(context: Context) {
        if (started) return
        started = true
        val repo = CategoryRepository(context.applicationContext)
        scope.launch { runCatching { repo.ensureSeeded() } }
        scope.launch { repo.observeCategories().catch { }.collect { categories = it } }
        scope.launch { repo.badgeFlow().catch { }.collect { badges = it } }
        scope.launch { repo.membersByCategoryFlow().catch { }.collect { membersByCategory = it } }
    }

    /** Badge của một số điện thoại (đọc trong composable → auto cập nhật). Dùng cho avatar. */
    fun badgesFor(number: String): List<CategoryBadge> = badges[PhoneKey.of(number)] ?: emptyList()

    /** Tập phoneKey thành viên của một nhóm — dùng cho bộ lọc CallListScreen. */
    fun membersOf(categoryId: Long): Set<String> = membersByCategory[categoryId] ?: emptySet()
}
