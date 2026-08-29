package com.antimobile.mcas.data.messaging.mms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

data class PreparedMmsImage(
    val sourceUri: String,
    val jpeg: ByteArray,
    val width: Int,
    val height: Int,
)

class MmsImageProcessor(private val context: Context) {
    fun prepare(uri: Uri, limits: MmsCarrierLimits, reservedTextBytes: Int): Result<PreparedMmsImage> = runCatching {
        val declaredType = context.contentResolver.getType(uri).orEmpty().lowercase()
        require(declaredType.startsWith("image/")) { "Selected content is not an image" }
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        var originalWidth = 0
        var originalHeight = 0
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            originalWidth = info.size.width
            originalHeight = info.size.height
            require(originalWidth in 1..20_000 && originalHeight in 1..20_000) { "Invalid image dimensions" }
            require(originalWidth.toLong() * originalHeight.toLong() <= 100_000_000L) { "Image is too large to decode safely" }
            val scale = min(
                1f,
                min(
                    limits.maxImageWidth.toFloat() / originalWidth,
                    limits.maxImageHeight.toFloat() / originalHeight,
                ),
            )
            decoder.setTargetSize(
                (originalWidth * scale).roundToInt().coerceAtLeast(1),
                (originalHeight * scale).roundToInt().coerceAtLeast(1),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
        }
        var bitmap = flattenTransparency(decoded)
        if (bitmap !== decoded) decoded.recycle()
        val budget = (limits.maxMessageBytes - reservedTextBytes - PDU_OVERHEAD_BYTES)
            .coerceAtLeast(MIN_IMAGE_BUDGET)
        var encoded = ByteArray(0)
        repeat(5) {
            for (quality in listOf(90, 82, 74, 66, 58, 50)) {
                encoded = ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
                    output.toByteArray()
                }
                if (encoded.size <= budget) {
                    val result = PreparedMmsImage(uri.toString(), encoded, bitmap.width, bitmap.height)
                    bitmap.recycle()
                    return@runCatching result
                }
            }
            val nextWidth = (bitmap.width * 0.78f).roundToInt().coerceAtLeast(320)
            val nextHeight = (bitmap.height * 0.78f).roundToInt().coerceAtLeast(240)
            require(nextWidth < bitmap.width || nextHeight < bitmap.height) { "Image cannot fit MMS limit" }
            val smaller = Bitmap.createScaledBitmap(bitmap, nextWidth, nextHeight, true)
            bitmap.recycle()
            bitmap = smaller
        }
        bitmap.recycle()
        error("Image cannot fit MMS limit")
    }

    private fun flattenTransparency(source: Bitmap): Bitmap {
        if (!source.hasAlpha()) return source
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).apply { drawColor(Color.WHITE); drawBitmap(source, 0f, 0f, null) }
        return result
    }

    companion object {
        private const val PDU_OVERHEAD_BYTES = 24 * 1024
        private const val MIN_IMAGE_BUDGET = 48 * 1024
    }
}
