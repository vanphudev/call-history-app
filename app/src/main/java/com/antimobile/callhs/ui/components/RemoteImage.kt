package com.antimobile.callhs.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.FieldSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Bộ tải ẢNH TỪ MẠNG gọn nhẹ (dự án KHÔNG dùng Coil/Glide): tự tải byte + giải mã [Bitmap], có cache
 * BỘ NHỚ ([LruCache] theo dung lượng) + cache ĐĨA (`cacheDir/remote_img`, tên = SHA-1 của URL). Dùng cho
 * ảnh QR VietQR (đổi theo số tiền) và logo app ngân hàng.
 *
 * MỌI thao tác tải/giải mã BẮT BUỘC ở luồng nền (đã bọc [Dispatchers.IO]). Thất bại/ngoại tuyến → `null`
 * (KHÔNG cache null → lần sau tự thử lại).
 */
object RemoteImageLoader {

    private const val DIR = "remote_img"
    private const val MAX_REDIRECTS = 5

    private val memCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(4 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    /** Ảnh đã có sẵn trong cache bộ nhớ (đọc đồng bộ, rẻ) — để hiện NGAY không nhấp nháy khi đổi số tiền. */
    fun cached(url: String): Bitmap? = memCache.get(url)

    /**
     * Tải (hoặc lấy từ cache) ảnh tại [url]. `null` nếu lỗi. Gọi ở luồng nền. [reqSizePx] (nếu có) là cạnh
     * đích mong muốn → giải mã GIẢM MẪU cho ảnh lớn (vd logo ngân hàng hiện trong ô ~52dp) để đỡ tốn bộ nhớ;
     * `null` = giải mã nguyên cỡ (dùng cho ảnh QR cần nét).
     */
    suspend fun load(context: Context, url: String, reqSizePx: Int? = null): Bitmap? = withContext(Dispatchers.IO) {
        memCache.get(url)?.let { return@withContext it }

        val dir = File(context.applicationContext.cacheDir, DIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, keyOf(url))
        if (file.exists() && file.length() > 0) {
            // Đọc CÓ BẢO VỆ: file cache có thể bị hệ thống dọn / người dùng "Xoá cache" ngay sau exists().
            runCatching { file.readBytes() }.getOrNull()?.let { decode(it, reqSizePx) }
                ?.let { memCache.put(url, it); return@withContext it }
        }

        val bytes = download(url) ?: return@withContext null
        val bmp = decode(bytes, reqSizePx) ?: return@withContext null
        runCatching { writeAtomic(file, bytes) }
        memCache.put(url, bmp)
        bmp
    }

    /** Tải sẵn [url] vào cache để lần hiển thị sau tức thì (bỏ qua kết quả). */
    suspend fun prefetch(context: Context, url: String, reqSizePx: Int? = null) {
        if (memCache.get(url) == null) runCatching { load(context, url, reqSizePx) }
    }

    /** Giải mã byte → bitmap; nếu [reqSizePx] > 0 thì giảm mẫu (inSampleSize) cho ảnh lớn hơn nhiều so với cỡ hiển thị. */
    private fun decode(bytes: ByteArray, reqSizePx: Int?): Bitmap? = runCatching {
        if (reqSizePx == null || reqSizePx <= 0) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            val maxWh = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxWh > 0 && maxWh / (sample * 2) >= reqSizePx) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    private fun download(url: String): ByteArray? {
        var current = url
        var hops = 0
        while (hops <= MAX_REDIRECTS) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "CallHS-Android")
                setRequestProperty("Accept", "image/*,*/*")
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: return null
                    current = URL(URL(current), loc).toString()
                    hops++
                    continue
                }
                if (code !in 200..299) return null
                val body = conn.inputStream.use { it.readBytes() }
                return if (body.isEmpty()) null else body
            } catch (_: Exception) {
                return null
            } finally {
                conn.disconnect()
            }
        }
        return null
    }

    /**
     * Ghi NGUYÊN TỬ dùng tên tạm DUY NHẤT mỗi lần (theo nanoTime): tránh trường hợp hai lần tải CÙNG url
     * chạy song song dùng chung một file `.tmp` khiến bên "thua" xoá nhầm file đích bên "thắng" vừa ghi.
     */
    private fun writeAtomic(dest: File, body: ByteArray) {
        val tmp = File(dest.parentFile, "${dest.name}.${System.nanoTime()}.tmp")
        try {
            tmp.writeBytes(body)
            if (!tmp.renameTo(dest)) {
                dest.delete()
                tmp.renameTo(dest)
            }
        } catch (_: Exception) {
            // best-effort; cache đĩa hỏng thì lần sau tự tải lại
        } finally {
            if (tmp.exists()) runCatching { tmp.delete() }
        }
    }

    private fun keyOf(url: String): String {
        val md = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        return md.joinToString("") { "%02x".format(it) }
    }
}

/** Trạng thái tải một ảnh mạng. */
sealed interface RemoteImageState {
    data object Loading : RemoteImageState
    data object Error : RemoteImageState
    class Success(val bitmap: Bitmap) : RemoteImageState {
        val image: ImageBitmap = bitmap.asImageBitmap()
    }
}

/**
 * Nạp ảnh tại [url] và trả về [RemoteImageState] (đọc cache bộ nhớ ĐỒNG BỘ trước → không loé khi có sẵn).
 * Tăng [reloadKey] để BUỘC tải lại (dùng cho nút "Thử lại").
 */
@Composable
fun rememberRemoteImage(url: String, reloadKey: Int = 0, reqSizePx: Int? = null): RemoteImageState {
    val context = LocalContext.current
    var state by remember(url, reloadKey) {
        mutableStateOf<RemoteImageState>(
            RemoteImageLoader.cached(url)?.let { RemoteImageState.Success(it) } ?: RemoteImageState.Loading
        )
    }
    androidx.compose.runtime.LaunchedEffect(url, reloadKey) {
        if (state is RemoteImageState.Success) return@LaunchedEffect
        state = RemoteImageState.Loading
        val bmp = RemoteImageLoader.load(context, url, reqSizePx)
        state = if (bmp != null) RemoteImageState.Success(bmp) else RemoteImageState.Error
    }
    return state
}

/**
 * Ảnh mạng bo góc DÙNG CHUNG (logo app ngân hàng…): shimmer khi tải, ảnh hiện có CROSSFADE khi xong,
 * [fallback] khi lỗi. Kích thước do [modifier] quyết định.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    reqSizePx: Int? = null,
    fallback: @Composable BoxScope.() -> Unit = {},
) {
    val state = rememberRemoteImage(url, reqSizePx = reqSizePx)
    Box(modifier) {
        Crossfade(targetState = state, animationSpec = tween(280), label = "remoteImage") { s ->
            when (s) {
                is RemoteImageState.Loading -> Box(Modifier.fillMaxSize().shimmer())
                is RemoteImageState.Error -> Box(Modifier.fillMaxSize(), content = fallback)
                is RemoteImageState.Success -> Image(
                    bitmap = s.image,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}

/** Lớp phủ SHIMMER (vệt sáng quét ngang) cho ô đang tải — tự đổi màu theo chế độ Sáng/Tối. */
@Composable
fun Modifier.shimmer(shape: Shape? = null): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val base = CardFill
    val highlight = FieldSurface
    val m = if (shape != null) this.clip(shape) else this
    return m.drawBehind {
        val w = size.width
        val travel = progress * 2f * w - w
        drawRect(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(travel, 0f),
                end = Offset(travel + w, size.height)
            )
        )
    }
}
