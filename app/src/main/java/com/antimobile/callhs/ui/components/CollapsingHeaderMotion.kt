package com.antimobile.callhs.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Hiệu ứng đổi nội dung thanh đầu trang khi cuộn (mặc định ↔ thu gọn), dùng chung
 * cho CallDetailScreen và AllCallsScreen.
 *
 * VÌ SAO viết riêng: bản cũ CROSSFADE theo alpha → ở LƯNG CHỪNG cả hai lớp (số điện
 * thoại vs avatar+tên) cùng mờ ~50% CHỒNG lên nhau, rất khó chịu nếu người dùng dừng
 * tay ngay điểm đó. Ở đây chuyển sang TRƯỢT DỌC kiểu bảng lật (odometer): mỗi lớp luôn
 * ĐỤC (opaque), chỉ đổi VỊ TRÍ chứ không bao giờ trong suốt → không còn cảnh hai lớp mờ
 * chồng nhau. Do kích hoạt theo NGƯỠNG (Boolean) nên trạng thái chuyển HẲN, không kẹt
 * nửa vời — nhưng vẫn có animation trượt uyển chuyển.
 *
 * targetState = true  (thu gọn): lớp mới trồi lên từ DƯỚI, lớp cũ trượt LÊN rồi ra.
 * targetState = false (mở rộng):  lớp mới hạ xuống từ TRÊN, lớp cũ trượt XUỐNG rồi ra.
 */
fun AnimatedContentTransitionScope<Boolean>.collapsingHeaderSwap(
    durationMillis: Int = 300
): ContentTransform {
    val spec = tween<IntOffset>(durationMillis, easing = FastOutSlowInEasing)
    return if (targetState) {
        slideInVertically(spec) { it } togetherWith slideOutVertically(spec) { -it }
    } else {
        slideInVertically(spec) { -it } togetherWith slideOutVertically(spec) { it }
    }
}
