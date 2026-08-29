package com.antimobile.mcas.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antimobile.mcas.data.model.CallType
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentGray
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.CallIncoming
import com.antimobile.mcas.ui.theme.CallMissed
import com.antimobile.mcas.ui.theme.CallOutgoing
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.CardShadow
import com.antimobile.mcas.ui.theme.CardSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.SimBadgeBg
import com.antimobile.mcas.ui.theme.SimBadgeText
import com.antimobile.mcas.ui.theme.ThemeSettings
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.ContactPhotoSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Nền avatar cho SỐ CHƯA LƯU danh bạ: xám XANH (blue-gray), PHẲNG (tĩnh, không blur/gradient); icon người trắng.
// Getter (không phải val cố định) để đổi theo chế độ Sáng/Tối — xem [ThemeSettings]/[Palette].
private val AvatarGray: Color get() = ThemeSettings.colors.avatarGray

private fun callTypeVector(type: CallType): ImageVector = when (type) {
    CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> Icons.AutoMirrored.Rounded.CallReceived
    CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
    CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> Icons.AutoMirrored.Rounded.CallMissed
    CallType.VOICEMAIL, CallType.UNKNOWN -> Icons.Rounded.Phone
}

fun callTypeColor(type: CallType): Color = when (type) {
    CallType.OUTGOING -> CallOutgoing
    CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> CallIncoming
    CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> CallMissed
    CallType.VOICEMAIL, CallType.UNKNOWN -> AccentGray
}

// Cache ảnh liên hệ ĐÃ giải mã (theo photoUri) ở cấp tiến trình. Nhờ nó, một bản Avatar dựng MỚI
// (vd item được "nhấc" lên trong CallContextMenuOverlay là bản sao của item đang hiện) dùng lại NGAY
// bitmap mà item gốc đã tải — không còn chớp avatar chữ-cái mặc định (xanh lá) rồi mới hiện ảnh.
// Giới hạn theo BYTE (~8MB, ảnh đã downsample rất nhỏ ~144px nên chứa được hàng trăm liên hệ).
private val PhotoCache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4).coerceAtLeast(1)
}

// Khoá cache có kèm "$generation|" nên mỗi lần danh bạ đổi, các bitmap thế hệ CŨ trở nên vô dụng nhưng vẫn
// nằm lại chiếm ngân sách 8MB. Đăng ký dọn sạch cache mỗi lần [ContactPhotoSignal.invalidate] để không rò bộ nhớ.
private val photoCacheFlushHook: Unit = run { ContactPhotoSignal.onInvalidate = { PhotoCache.evictAll() } }

/** Trạng thái ảnh liên hệ: [bitmap] khi đã có; [loading] = ĐANG giải mã (chỉ đúng khi có URI & cache lạnh). */
data class ContactPhotoState(val bitmap: ImageBitmap?, val loading: Boolean)

@Composable
fun rememberContactPhoto(photoUri: String?): ContactPhotoState {
    val context = LocalContext.current
    val key = photoUri?.takeIf { it.isNotBlank() }
    // THẾ HỆ danh bạ: đưa vào KHOÁ cache để khi danh bạ đổi (kể cả CHỈ thay ảnh mà URI giữ nguyên) thì
    // giải mã lại ảnh mới thay vì trả ảnh cũ. Đọc dạng State → tự recompose khi ContactsObserver báo đổi.
    val generation = ContactPhotoSignal.generation.intValue
    val cacheKey = key?.let { "$generation|$it" }
    // Đọc cache ĐỒNG BỘ ngay trong lúc compose: nếu ảnh (của thế hệ hiện tại) đã tải trước đó (vd bởi item
    // gốc trong danh sách) thì bản sao này (vd item "nhấc" lên ở context-menu) hiện ảnh NGAY frame đầu → không chớp.
    val cached = remember(cacheKey) { cacheKey?.let { PhotoCache.get(it) } }
    // [bitmap]/[loading] khoá theo [key] (KHÔNG theo thế hệ) → khi danh bạ đổi, ảnh ĐANG hiện được GIỮ lại
    // trên màn trong lúc giải mã ảnh mới ở nền rồi mới tráo → không chớp về ô xám dù đã đổi thế hệ.
    var bitmap by remember(key) { mutableStateOf(cached) }
    // Đang tải = có URI nhưng cache lạnh (chưa có bitmap). Cờ này TẮT khi decode xong DÙ thành công
    // HAY THẤT BẠI → decode lỗi (ảnh bị xoá/không đọc được) sẽ rơi về fallback chữ-cái/icon, KHÔNG
    // kẹt mãi ở ô xám trống.
    var loading by remember(key) { mutableStateOf(key != null && cached == null) }
    LaunchedEffect(cacheKey) {
        if (key == null) { loading = false; return@LaunchedEffect }             // không có ảnh
        if (cached != null) { bitmap = cached; loading = false; return@LaunchedEffect } // đã có ảnh của thế hệ này
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(key)
                // Đọc kích thước trước để downsample về ~144px (avatar lớn nhất ~72dp) — tránh
                // giữ bitmap full-res gây OOM khi cuộn danh sách dài.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val target = 144
                var sample = 1
                val min = minOf(bounds.outWidth, bounds.outHeight)
                if (min > 0) while (min / (sample * 2) >= target) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
        if (decoded != null) {
            cacheKey?.let { PhotoCache.put(it, decoded) }
            bitmap = decoded
        }
        loading = false // xong lượt decode (kể cả thất bại) → thôi placeholder, nhường fallback chữ/icon
    }
    return ContactPhotoState(bitmap, loading)
}

/**
 * Avatar: ảnh liên hệ → chữ cái (có tên) → ICON KHẨN CẤP (113/114/115 chưa lưu danh bạ) → icon người (số lạ).
 *
 * [specialIconRes] (tuỳ chọn): huy hiệu ngành cho số khẩn cấp — CHỈ hiện khi số CHƯA phải liên hệ (không có
 * ảnh & [isNamed] = false), nhờ vậy liên hệ đã lưu vẫn luôn được ưu tiên (ảnh/chữ cái danh bạ).
 */
@Composable
fun Avatar(
    label: String,
    photoUri: String?,
    isNamed: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    @DrawableRes specialIconRes: Int? = null
) {
    val photoState = rememberContactPhoto(photoUri)
    val photo = photoState.bitmap
    when {
        photo != null -> Image(
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )

        // ĐANG tải ảnh liên hệ (cache lạnh) → nền xám PHẲNG trung tính, KHÔNG hiện chữ-cái xanh lá, để
        // khi ảnh hiện ra không chớp đổi màu. Cache ấm (vd bản sao trong context-menu) thì bỏ qua nhánh
        // này. Khi decode XONG mà lỗi (ảnh bị xoá) thì loading=false → rơi xuống fallback chữ/icon dưới.
        photoState.loading -> Box(
            modifier = modifier.size(size).clip(CircleShape).background(AvatarGray)
        )

        isNamed -> Box(
            // Liên hệ ĐÃ lưu danh bạ → nền XANH LÁ chủ đạo của dự án, chữ cái trắng.
            modifier = modifier.size(size).clip(CircleShape).background(Primary),
            contentAlignment = Alignment.Center
        ) {
            val letter = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "#"
            Text(
                letter, color = Color.White, fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.4f).sp
            )
        }

        specialIconRes != null -> Box(
            // Số KHẨN CẤP chưa lưu danh bạ → huy hiệu ngành trong vòng tròn nền xám RẤT NHẠT (CardFill) để
            // đường tròn vẫn hiện rõ trên thẻ trắng (app không dùng viền); Fit để không cắt mất chi tiết huy hiệu.
            modifier = modifier.size(size).clip(CircleShape).background(CardFill),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(specialIconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.82f)
            )
        }

        else -> Box(
            // Số CHƯA lưu danh bạ → nền xám, icon người trắng.
            modifier = modifier.size(size).clip(CircleShape).background(AvatarGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
        }
    }
}

/** Icon loại cuộc gọi (mũi tên/camera). Mặc định tô theo loại; truyền [tint] để ép màu (vd theo kết nối). */
@Composable
fun CallTypeIcon(type: CallType, modifier: Modifier = Modifier, size: Dp = 18.dp, isVideo: Boolean = false, tint: Color? = null) {
    val color = tint ?: if (isVideo) AccentBlue else callTypeColor(type)
    if (isVideo) {
        Icon(Icons.Rounded.Videocam, null, tint = color, modifier = modifier.size(size))
    } else {
        Icon(
            imageVector = callTypeVector(type),
            contentDescription = null,
            tint = color,
            modifier = modifier.size(size)
        )
    }
}

/** Huy hiệu SIM xanh lá nhạt ("SIM 1"). */
@Composable
fun SimBadge(label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SimBadgeBg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(label, color = SimBadgeText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/** Thẻ nền TRẮNG, bo góc, KHÔNG viền — dùng bóng mềm để tách nền, cho cảm giác mượt. */
@Composable
fun PanelCard(modifier: Modifier = Modifier, radius: Dp = 20.dp, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(radius), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(radius))
            .background(CardSurface)
    ) { content() }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, text: String = appStrings().callList.loadingCalls) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Primary)
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Phone,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(CardFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiaryRef,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PermissionState(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttonLabel: String = appStrings().common.allowAccess
) {
    val s = appStrings().callList
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Huy hiệu tròn xanh lá — PHẲNG (không halo/gradient), đồng bộ tone chủ đạo.
        Box(
            modifier = Modifier.size(104.dp).clip(CircleShape).background(AccentGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Phone,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = s.permTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = s.permBody,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        // Thẻ trấn an: TRẮNG, bóng mềm, KHÔNG viền (đồng bộ toàn app).
        PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                PermissionBullet(Icons.Rounded.Lock, s.permBullet1)
                PermissionBullet(Icons.Rounded.VerifiedUser, s.permBullet2)
                PermissionBullet(Icons.Rounded.Visibility, s.permBullet3)
            }
        }
        Spacer(Modifier.height(26.dp))
        // CTA chính: pill xanh lá đặc, chữ trắng, bóng nhẹ để nổi khối.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
                .clip(RoundedCornerShape(28.dp))
                .background(Primary)
                .clickable { onRequest() }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = s.permRevoke,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Banner MỜI cấp quyền Danh bạ (TUỲ CHỌN) — hiện phía trên danh sách khi chưa cấp.
 * [onAllow] xin quyền (hoặc mở Cài đặt nếu đã từ chối vĩnh viễn); [onDismiss] ẩn banner.
 */
@Composable
fun ContactsPermissionBanner(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    allowLabel: String = appStrings().common.grantPermission,
    modifier: Modifier = Modifier
) {
    val s = appStrings()
    PanelCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), radius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(AccentGreenBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, null, tint = Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.callList.bannerTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = s.callList.bannerBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary)
                    .clickable(onClick = onAllow)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = allowLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = s.common.dismiss, tint = AccentGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Một dòng trấn an trong màn xin quyền: chip icon tròn xanh lá + mô tả ngắn. */
@Composable
private fun PermissionBullet(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(AccentGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

// Getter để bám theo [TextSecondary] động (Sáng/Tối) thay vì đóng băng giá trị lúc khởi tạo.
private val TextTertiaryRef: Color get() = TextSecondary.copy(alpha = 0.6f)
