package com.antimobile.callhs.util

import com.antimobile.callhs.i18n.appStrings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Định dạng thời gian cho toàn app — THỐNG NHẤT dùng 24 GIỜ (không AM/PM, không SA/CH).
 */
object TimeFormat {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val vi: Locale = Locale.forLanguageTag("vi-VN")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", vi)         // 24h — giờ:phút
    private val secsClockFmt = DateTimeFormatter.ofPattern("HH:mm:ss", vi) // 24h — giờ:phút:giây
    private val ddMMFmt = DateTimeFormatter.ofPattern("dd/MM", vi)
    private val ddMMyyyyFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", vi)

    /** Giờ trong ngày "HH:mm" (24h). */
    fun time(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(timeFmt)

    /**
     * Mốc thời gian TUYỆT ĐỐI cho từng item: LUÔN hiện giờ:phút:giây (24h); KHÔNG dùng "thứ"
     * hay "x phút/giờ trước". Kèm ngày (dd/MM hoặc dd/MM/yyyy) khi không phải hôm nay để khỏi nhập nhằng.
     */
    fun itemClock(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        val that = zdt.toLocalDate()
        val clock = zdt.format(secsClockFmt)
        return when {
            that.isEqual(today) -> clock
            that.year == today.year -> "${that.format(ddMMFmt)} $clock"
            else -> "${that.format(ddMMyyyyFmt)} $clock"
        }
    }

    /** Ngày giờ đầy đủ đến từng giây cho dropdown chi tiết (không dùng "thứ"): "30/06/2026 · 09:24:15". */
    fun fullDateTimeWithSeconds(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(zone)
        return "${zdt.format(ddMMyyyyFmt)} · ${zdt.format(secsClockFmt)}"
    }

    /** Chỉ NGÀY: "18/06" (năm nay) / "18/06/2024" (năm khác) — không giờ. */
    fun date(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        val that = zdt.toLocalDate()
        return if (that.year == today.year) that.format(ddMMFmt) else that.format(ddMMyyyyFmt)
    }

    /** "18/06 21:41" (năm nay) / "18/06/2024 21:41" (cũ hơn) — ngày + giờ:phút 24h, không giây. */
    fun dayClock(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        val that = zdt.toLocalDate()
        val clock = zdt.format(timeFmt)
        return if (that.year == today.year) "${that.format(ddMMFmt)} $clock" else "${that.format(ddMMyyyyFmt)} $clock"
    }

    /** Dạng "Hôm nay, 10:18" cho dòng hoạt động gần đây. */
    fun activityDateTime(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(zone)
        return "${dayPart(date.toLocalDate())}, ${date.format(timeFmt)}"
    }

    /**
     * Nhãn nhóm ngày cho danh sách: chỉ "Hôm nay" / "Hôm qua"; còn lại là "Thứ · dd/MM/yyyy" (vd
     * "Thứ 4 · 18/06/2024"). ĐA NGÔN NGỮ: chữ lấy từ [appStrings]; phần ngày (dd/MM/yyyy) giữ nguyên định dạng số.
     */
    fun sectionLabel(millis: Long): String {
        val s = appStrings().datetime
        val today = LocalDate.now(zone)
        val that = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return when {
            that.isEqual(today) -> s.today
            that.isEqual(today.minusDays(1)) -> s.yesterday
            else -> "${s.weekdayShort(that.dayOfWeek)} · ${that.format(ddMMyyyyFmt)}"
        }
    }

    private fun dayPart(that: LocalDate): String {
        val s = appStrings().datetime
        val today = LocalDate.now(zone)
        return when {
            that.isEqual(today) -> s.today
            that.isEqual(today.minusDays(1)) -> s.yesterday
            ChronoUnit.DAYS.between(that, today) < 7 -> s.weekdayShort(that.dayOfWeek)
            that.year == today.year -> that.format(ddMMFmt)
            else -> that.format(ddMMyyyyFmt)
        }
    }

    /** Thời lượng dạng "X phút Y giây" / "Y giây" (ĐA NGÔN NGỮ qua [appStrings]); chuỗi rỗng nếu 0. */
    fun durationLabel(durationSeconds: Long): String {
        if (durationSeconds <= 0) return ""
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return appStrings().datetime.duration(minutes, seconds)
    }

    /** Thời lượng gọn cho ô thống kê nhỏ: "1g 36p" / "36p" / "45s" (ĐA NGÔN NGỮ — đơn vị từ [appStrings]). */
    fun durationCompact(durationSeconds: Long): String {
        val u = appStrings().datetime
        if (durationSeconds <= 0) return "0${u.unitMinuteShort}"
        val h = durationSeconds / 3600
        val m = (durationSeconds % 3600) / 60
        val s = durationSeconds % 60
        return when {
            h > 0 -> if (m > 0) "${h}${u.unitHourShort} ${m}${u.unitMinuteShort}" else "${h}${u.unitHourShort}"
            m > 0 -> "${m}${u.unitMinuteShort}"
            else -> "${s}${u.unitSecondShort}"
        }
    }

    /** Thời lượng dạng "1 giờ 36 phút" cho thống kê (ĐA NGÔN NGỮ). "0 giây" nếu 0. */
    fun durationVerboseHours(durationSeconds: Long): String {
        val u = appStrings().datetime
        val zero = "0 ${u.unitSecond}"
        if (durationSeconds <= 0) return zero
        val h = durationSeconds / 3600
        val m = (durationSeconds % 3600) / 60
        val s = durationSeconds % 60
        val parts = ArrayList<String>(3)
        if (h > 0) parts.add("$h ${u.unitHour}")
        if (m > 0) parts.add("$m ${u.unitMinute}")
        if (s > 0 && m == 0L) parts.add("$s ${u.unitSecond}") // hiện giây khi không có phút (kể cả khi có giờ)
        return parts.joinToString(" ").ifEmpty { zero }
    }

    /** Khung giờ hoạt động "09:05 – 21:47" — mốc SỚM NHẤT–MUỘN NHẤT trong ngày, chính xác đến PHÚT (24h). */
    fun activeHoursRange(millisList: List<Long>): String {
        if (millisList.isEmpty()) return "—"
        val times = millisList.map { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
        return "${times.min().format(timeFmt)} – ${times.max().format(timeFmt)}"
    }

    /**
     * KHOẢNG CÁCH giữa hai cuộc gọi liên tiếp — "tổng thời gian" hiện trên chip DÒNG THỜI GIAN.
     *
     * Đơn vị GỐC là phút; TỰ nâng cấp lên đơn vị lớn hơn khi đủ: ≥ 60 phút → "giờ" (kèm phút lẻ),
     * ≥ 24 giờ → "ngày" (kèm giờ lẻ). CỐ TÌNH KHÔNG lên tháng/năm (vd 400 ngày vẫn hiện "400 ngày").
     * Dưới 1 phút → "giây". Tối đa 2 đơn vị cho gọn. ĐA NGÔN NGỮ qua [appStrings] (đơn vị đổi theo ngôn ngữ).
     */
    fun gapLabel(deltaMillis: Long): String {
        val u = appStrings().datetime
        val totalSec = (deltaMillis / 1000).coerceAtLeast(0)
        if (totalSec < 60) return "$totalSec ${u.unitSecond}"
        val totalMin = totalSec / 60
        if (totalMin < 60) return "$totalMin ${u.unitMinute}"
        val totalHour = totalMin / 60
        if (totalHour < 24) {
            val m = totalMin % 60
            return if (m > 0) "$totalHour ${u.unitHour} $m ${u.unitMinute}" else "$totalHour ${u.unitHour}"
        }
        val days = totalHour / 24
        val h = totalHour % 24
        return if (h > 0) "$days ${u.unitDay} $h ${u.unitHour}" else "$days ${u.unitDay}"
    }
}
