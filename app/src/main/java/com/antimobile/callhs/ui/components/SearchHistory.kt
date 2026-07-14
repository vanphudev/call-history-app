package com.antimobile.callhs.ui.components

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.LinkColor
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.TimeFormat
import org.json.JSONArray
import org.json.JSONObject

// =====================================================================================================
// LỊCH SỬ TÌM KIẾM dùng CHUNG (nhật ký cuộc gọi + danh bạ). Lưu trong SharedPreferences dạng JSON, tối đa
// [SEARCH_HISTORY_MAX] mục (mới → cũ). Mỗi màn dùng FILE prefs RIÊNG (vd "call_list_prefs" vs "contacts_prefs")
// nên hai lịch sử ĐỘC LẬP dù chung khoá.
// =====================================================================================================

/** Một lần tìm kiếm đã lưu: từ khoá + thời điểm. */
data class SearchEntry(val query: String, val time: Long)

private const val SEARCH_HISTORY_KEY = "search_history"
const val SEARCH_HISTORY_MAX = 5

/** Đọc lịch sử tìm kiếm (mới → cũ) từ prefs (JSON). */
fun SharedPreferences.loadSearchHistory(): List<SearchEntry> {
    val json = getString(SEARCH_HISTORY_KEY, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SearchEntry(o.getString("q"), o.getLong("t"))
        }
    }.getOrDefault(emptyList())
}

fun SharedPreferences.saveSearchHistory(list: List<SearchEntry>) {
    val arr = JSONArray()
    list.forEach { arr.put(JSONObject().put("q", it.query).put("t", it.time)) }
    edit().putString(SEARCH_HISTORY_KEY, arr.toString()).apply()
}

/** Đưa [query] lên ĐẦU lịch sử (trùng → gộp & cập nhật thời gian), tối đa [SEARCH_HISTORY_MAX] (bỏ cũ nhất). */
fun pushSearch(current: List<SearchEntry>, query: String, now: Long): List<SearchEntry> {
    val q = query.trim()
    if (q.isBlank()) return current
    val deduped = current.filterNot { it.query.equals(q, ignoreCase = true) }
    return (listOf(SearchEntry(q, now)) + deduped).take(SEARCH_HISTORY_MAX)
}

/**
 * Danh sách LỊCH SỬ tìm kiếm (tối đa 5) trong KHUNG CARD: mỗi dòng = icon + từ khoá + thời gian tìm.
 * Chạm dòng → điền lại ô nhập + tìm luôn; nút X (tròn xám nhạt) ở CUỐI mỗi dòng để xoá. Card chỉ CAO tối đa vừa vùng
 * trống (trên bàn phím) — nếu không đủ chỗ thì CUỘN bên trong (dùng weight fill=false + verticalScroll).
 */
@Composable
fun SearchHistoryView(
    history: List<SearchEntry>,
    onTap: (SearchEntry) -> Unit,
    onDelete: (SearchEntry) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = appStrings().callList
    if (history.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.History, null, tint = TextSecondary.copy(alpha = 0.55f), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text(s.searchRecentEmpty, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        return
    }
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.searchRecentTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = s.clearAll,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = LinkColor,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onClearAll).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        PanelCard(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), radius = 18.dp) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                history.forEach { entry ->
                    SearchHistoryRow(entry = entry, onClick = { onTap(entry) }, onDelete = { onDelete(entry) })
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryRow(entry: SearchEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.History, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.query,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(TimeFormat.dayClock(entry.time), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        // Nút X nhỏ ở CUỐI dòng: tròn nền xám NHẠT, icon xám — chạm để xoá dòng này (thay cử chỉ vuốt cũ).
        // clickable riêng của nút "nuốt" chạm nên KHÔNG kích hoạt onClick của cả dòng.
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(AccentGrayBg).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = appStrings().callList.delete, tint = AccentGray, modifier = Modifier.size(16.dp))
        }
    }
}
