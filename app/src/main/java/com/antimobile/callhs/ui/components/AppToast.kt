package com.antimobile.callhs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.CardShadow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Trạng thái trực quan của toast; màu và icon được ánh xạ tự động theo theme của ứng dụng. */
enum class AppToastType { Info, Success, Warning, Error }

/** Thời gian hiển thị cơ sở; hệ thống trợ năng có thể tự tăng thời gian này khi cần. */
enum class AppToastDuration(internal val millis: Long) {
    Short(2_400L),
    Long(4_000L)
}

internal data class AppToastMessage(
    val id: Long,
    val text: String,
    val type: AppToastType,
    val duration: AppToastDuration,
    val shownAtMillis: Long = 0L
)

/**
 * API toast dùng chung toàn ứng dụng. Có thể gọi từ Composable, ViewModel callback hoặc utility mà không cần
 * giữ `Context`: `AppToast.show("Đã lưu", AppToastType.Success)`.
 *
 * Queue và nội dung hiện tại được giữ ở đây để toast không biến mất khi bottom sheet/dialog vừa đóng. Host ở
 * cửa sổ trên cùng tự nhận quyền hiển thị, sau đó host gốc tiếp quản phần thời gian còn lại khi modal đóng.
 */
object AppToast {
    private val nextMessageId = AtomicLong(0L)
    private val nextHostId = AtomicLong(0L)
    private val hostLock = Any()
    private val hosts = mutableListOf<Long>()

    private val messages = MutableSharedFlow<AppToastMessage>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _current = MutableStateFlow<AppToastMessage?>(null)
    private val _topHostId = MutableStateFlow<Long?>(null)

    private val current = _current.asStateFlow()
    private val topHostId = _topHostId.asStateFlow()

    fun show(
        message: String,
        type: AppToastType = AppToastType.Info,
        duration: AppToastDuration = AppToastDuration.Short
    ) {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) return
        messages.tryEmit(
            AppToastMessage(
                id = nextMessageId.incrementAndGet(),
                text = cleanMessage,
                type = type,
                duration = duration
            )
        )
    }

    private fun newHostId(): Long = nextHostId.incrementAndGet()

    private fun attachHost(id: Long) = synchronized(hostLock) {
        hosts += id
        _topHostId.value = id
    }

    private fun detachHost(id: Long) = synchronized(hostLock) {
        hosts.remove(id)
        _topHostId.value = hosts.lastOrNull()
    }

    @Composable
    internal fun BindHost(): Pair<AppToastMessage?, Boolean> {
        val hostId = remember { newHostId() }
        DisposableEffect(hostId) {
            attachHost(hostId)
            onDispose { detachHost(hostId) }
        }
        val message by current.collectAsState()
        val activeHostId by topHostId.collectAsState()
        return message to (activeHostId == hostId)
    }

    @Composable
    internal fun RunQueue() {
        val accessibilityManager = LocalAccessibilityManager.current
        LaunchedEffect(accessibilityManager) {
            messages.collect { message ->
                // Ghi giờ khi toast THỰC SỰ tới lượt hiển thị, không phải lúc vừa được đưa vào queue.
                // Nhờ vậy các toast liên tiếp có mốc giây khác nhau và người dùng dễ nhận biết từng thông báo.
                val shownMessage = message.copy(shownAtMillis = System.currentTimeMillis())
                _current.value = shownMessage
                val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
                    originalTimeoutMillis = shownMessage.duration.millis,
                    containsIcons = true,
                    containsText = true,
                    containsControls = false
                ) ?: message.duration.millis
                delay(timeout)
                _current.value = null
                // Để hoạt ảnh thoát kết thúc trước khi lấy toast tiếp theo trong queue.
                delay(EXIT_MILLIS.toLong())
            }
        }
    }
}

/**
 * Host hiển thị toast. Đặt một host gốc với [isRootHost] = true ở lớp trên cùng của Activity; các cửa sổ
 * Compose riêng (dialog/bottom sheet) đặt thêm host thường để toast luôn nằm trên modal đang mở.
 */
@Composable
fun AppToastHost(
    modifier: Modifier = Modifier,
    isRootHost: Boolean = false
) {
    if (isRootHost) AppToast.RunQueue()
    val (message, ownsPresentation) = AppToast.BindHost()
    val visible = message != null && ownsPresentation

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(ENTER_MILLIS, easing = FastOutSlowInEasing),
                initialOffsetY = { -it / 2 }
            ) + fadeIn(tween(ENTER_MILLIS)) + scaleIn(tween(ENTER_MILLIS), initialScale = 0.96f),
            exit = slideOutVertically(
                animationSpec = tween(EXIT_MILLIS),
                targetOffsetY = { -it / 3 }
            ) + fadeOut(tween(EXIT_MILLIS)) + scaleOut(tween(EXIT_MILLIS), targetScale = 0.98f)
        ) {
            message?.let { AppToastCard(it) }
        }
    }
}

@Composable
private fun AppToastCard(message: AppToastMessage) {
    val visual = toastVisual(message.type)
    val shape = RoundedCornerShape(18.dp)
    val shownAt = remember(message.id, message.shownAtMillis) {
        ToastDateTimeFormatter.format(
            Instant.ofEpochMilli(message.shownAtMillis.coerceAtLeast(0L)).atZone(ZoneId.systemDefault())
        )
    }
    Row(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = CardShadow,
                spotColor = CardShadow
            )
            .clip(shape)
            .background(ToastSurface)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(visual.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.foreground,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = message.text,
                color = ToastText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = shownAt,
                color = ToastDateTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                ),
                maxLines = 1,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

private data class ToastVisual(
    val icon: ImageVector,
    val foreground: Color,
    val background: Color
)

@Composable
private fun toastVisual(type: AppToastType): ToastVisual = when (type) {
    AppToastType.Info -> ToastVisual(Icons.Rounded.Info, AccentBlue, AccentBlueBg)
    AppToastType.Success -> ToastVisual(Icons.Rounded.CheckCircle, AccentGreen, AccentGreenBg)
    AppToastType.Warning -> ToastVisual(Icons.Rounded.Warning, AccentAmber, AccentAmberBg)
    AppToastType.Error -> ToastVisual(Icons.Rounded.Error, AccentRed, AccentRedBg)
}

private const val ENTER_MILLIS = 220
private const val EXIT_MILLIS = 170

// Trắng lạnh nhẹ để toast tách khỏi nền trắng thuần #FFFFFF của app nhưng vẫn giữ cảm giác sạch, sáng.
private val ToastSurface = Color(0xFFF7F9FC)
private val ToastText = Color(0xFF202124)
private val ToastDateTime = Color(0xFF7A8088)
private val ToastDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm:ss")
