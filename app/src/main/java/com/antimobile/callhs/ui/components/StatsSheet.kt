package com.antimobile.callhs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallResults
import com.antimobile.callhs.util.SimScope
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.formatPhone

/** Ước lượng dự phòng cho peek (thực tế đo chiều cao thật của handle lúc chạy). */
val STATS_HANDLE_HEIGHT = 70.dp

/**
 * Tay cầm bottom sheet thống kê — luôn thấy khi thu gọn, 2 dòng:
 * line 1 = Thống kê · số (đậm màu) · nhà mạng (tag); line 2 = xem [handleSubline] (tổng cuộc + gồm mấy SIM +
 * SIM/giờ của cuộc gần nhất). Giữ ĐÚNG 2 dòng (line 2 maxLines=1) để không vượt chiều cao peek cố định.
 *
 * [onOpenStats] (tuỳ chọn): khi != null, hiện NÚT TRÒN bên PHẢI (biểu tượng biểu đồ tròn) mở màn "Phân tích
 * cuộc gọi" chi tiết. Nút cao 40dp nằm gọn trong hàng 2 dòng nên KHÔNG tăng chiều cao peek cố định.
 */
@Composable
fun StatsSheetHandle(detail: CallNumberDetail, modifier: Modifier = Modifier, onOpenStats: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHandleBar()
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.InsertChart, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Line 1: Thống kê · số (đậm màu) · nhà mạng (tag).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.SemiBold)) { append(appStrings().callDetail.statsTitle) }
                            withStyle(SpanStyle(color = TextSecondary)) { append("  ·  ") }
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append(formatPhone(detail.number)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    detail.carrier?.let {
                        Spacer(Modifier.width(8.dp))
                        HandleTag(it)
                    }
                }
                Spacer(Modifier.height(2.dp))
                // Line 2: Tổng {n} cuộc gọi · Gọi cuối lúc <ngày giờ:phút 24h>.
                Text(
                    text = handleSubline(detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onOpenStats != null) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandSoft)
                        .clickable(onClick = onOpenStats),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.DonutLarge, contentDescription = appStrings().phoneStats.openDesc, tint = Primary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/**
 * Dòng phụ tay cầm: "Tổng {n} cuộc gọi [(Gồm 2 SIM)] · Gọi cuối [(SIM 1)] lúc <giờ>".
 * Số này gọi qua ≥2 SIM (đang lắp) → thêm "(Gồm N SIM)" và ghi SIM của cuộc GẦN NHẤT (nếu cuộc đó thuộc SIM
 * đang lắp; cuộc từ SIM đã tháo có simLabel = null → bỏ phần SIM, hiện giờ như thường).
 */
private fun handleSubline(detail: CallNumberDetail): String {
    val cd = appStrings().callDetail
    val multiSim = detail.simLabels.size >= 2
    val sb = StringBuilder(cd.totalCalls(detail.totalCalls))
    if (multiSim) sb.append("  ").append(cd.inclSims(detail.simLabels.size))
    val last = detail.lastCallMillis
    if (last != null) {
        val time = TimeFormat.dayClock(last)
        val latestSim = detail.entries.firstOrNull()?.simLabel
        sb.append("  ·  ")
        if (multiSim && latestSim != null) sb.append(cd.lastCallAtSim(latestSim, time))
        else sb.append(cd.lastCallAt(time))
    }
    return sb.toString()
}

/** Tag nhỏ (pill) cho nhà mạng ở tay cầm thống kê. */
@Composable
private fun HandleTag(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CardFill).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * Nội dung đầy đủ của bottom sheet thống kê (hiện khi vuốt lên).
 *
 * Có thanh chọn SIM (segmented) để xem số liệu TỔNG HỢP ("Tất cả") hay RIÊNG từng SIM: chọn "SIM 1"/"SIM 2" thì
 * TOÀN BỘ khối số liệu (tổng cuộc, gọi đi/đến/nhỡ, thời lượng, khung giờ, gọi cuối) tính lại chỉ trên cuộc của
 * SIM đó. Đang khoá PHẠM VI SIM toàn app → hiện "Đang xem SIM X" (dữ liệu đã theo SIM đó). Số này chỉ 1 SIM →
 * không hiện thanh (không có gì để tách). Số liệu tính từ [detail.entries] nên "Tất cả" khớp đúng số tổng cũ.
 */
@Composable
fun StatsSheetContent(detail: CallNumberDetail, bottomInset: Dp, modifier: Modifier = Modifier) {
    val cd = appStrings().callDetail
    val globalSim = SimScope.effectiveLabel
    val simLabels = detail.simLabels   // các SIM (đang lắp) mà số này đã gọi qua; đã sort, không null
    var statsSim by remember(detail.number) { mutableStateOf<String?>(null) }
    // Khoá phạm vi toàn app → bỏ lọc cục bộ (dữ liệu đã theo SIM đó). Ngược lại lọc theo lựa chọn (chỉ giữ nếu
    // SIM còn trong danh sách). null = tất cả → tính trên toàn bộ (khớp số tổng như trước).
    val effectiveStatsSim = if (globalSim != null) null else statsSim?.takeIf { it in simLabels }
    val st = remember(detail.entries, effectiveStatsSim) {
        val es = effectiveStatsSim?.let { l -> detail.entries.filter { it.simLabel == l } } ?: detail.entries
        statsOf(es)
    }
    Column(modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp).padding(top = 22.dp, bottom = bottomInset + 22.dp)) {
        if (globalSim != null || simLabels.size >= 2) {
            // Nhiều hơn 2 SIM (rất hiếm cho MỘT số) → cho cuộn ngang để không ô nào bị cắt.
            val rowMod = if (simLabels.size > 2) {
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 18.dp)
            } else {
                Modifier.fillMaxWidth().padding(bottom = 18.dp)
            }
            Row(modifier = rowMod, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                SimScopeControl(
                    availableLabels = simLabels,
                    localSelection = effectiveStatsSim,   // đã chuẩn hoá → ô tô sáng luôn khớp số liệu đang hiện
                    onLocalSelect = { statsSim = it },
                    allLabel = appStrings().callList.filterAll
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(Icons.Rounded.Phone, AccentBlue, AccentBlueBg, cd.metricCalls, st.totalCalls.toString(), Modifier.weight(1f))
            Metric(Icons.Rounded.Schedule, AccentGreen, AccentGreenBg, cd.metricDuration, TimeFormat.durationCompact(st.duration), Modifier.weight(1f))
            Metric(Icons.AutoMirrored.Rounded.CallMissed, AccentRed, AccentRedBg, cd.metricMissed, st.missed.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        InfoLine(Icons.AutoMirrored.Rounded.CallMade, AccentGreen, cd.statOutgoing, st.outgoing.toString())
        Spacer(Modifier.height(12.dp))
        InfoLine(Icons.AutoMirrored.Rounded.CallReceived, AccentBlue, cd.statIncoming, st.incoming.toString())
        Spacer(Modifier.height(12.dp))
        InfoLine(Icons.Rounded.Schedule, TextSecondary, cd.statTotalTime, TimeFormat.durationVerboseHours(st.duration))
        Spacer(Modifier.height(12.dp))
        InfoLine(Icons.Rounded.NotificationsActive, TextSecondary, cd.statActiveHours, TimeFormat.activeHoursRange(st.dateMillis))
        st.lastCallMillis?.let {
            Spacer(Modifier.height(12.dp))
            InfoLine(Icons.Rounded.Phone, TextSecondary, cd.statLastCall, TimeFormat.activityDateTime(it))
        }
    }
}

/** Số liệu thống kê tính từ một tập cuộc gọi (toàn bộ, hoặc đã lọc theo SIM). */
private class SheetStats(
    val totalCalls: Int,
    val duration: Long,
    val missed: Int,
    val outgoing: Int,
    val incoming: Int,
    val dateMillis: List<Long>,
    val lastCallMillis: Long?
)

/**
 * Tính số liệu cho [entries] (đã sort mới→cũ) — CÙNG quy tắc với [com.antimobile.callhs.data.repository.CallLogRepository.loadDetail]:
 * nhỡ = MISSED/REJECTED/BLOCKED; đến = INCOMING/ANSWERED_EXTERNALLY; thời lượng chỉ cộng cuộc ĐÃ đàm thoại.
 * Nhờ vậy "Tất cả" (entries = detail.entries) khớp đúng số tổng repo đã tính.
 */
private fun statsOf(entries: List<CallEntry>): SheetStats {
    var dur = 0L
    var missed = 0
    var out = 0
    var inc = 0
    for (e in entries) {
        when (e.type) {
            CallType.OUTGOING -> out++
            CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> inc++
            CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> missed++
            else -> {}
        }
        if (CallResults.isConnected(e.type, e.durationSeconds)) dur += e.durationSeconds
    }
    return SheetStats(
        totalCalls = entries.size,
        duration = dur,
        missed = missed,
        outgoing = out,
        incoming = inc,
        dateMillis = entries.map { it.dateMillis },
        lastCallMillis = entries.firstOrNull()?.dateMillis
    )
}

@Composable
private fun Metric(icon: ImageVector, tint: Color, bg: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoLine(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
