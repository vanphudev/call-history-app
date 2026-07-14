package com.antimobile.callhs.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.antimobile.callhs.R
import kotlin.math.sin

/**
 * Phát tiếng "tạch" NGẮN, độ trễ thấp cho thanh mục lục A→Z: mỗi lần trượt sang một chữ cái mới kêu MỘT
 * tiếng như lẫy bánh răng. Dùng [SoundPool] (chuyên cho âm UI ngắn, phát chồng được) thay vì MediaPlayer để
 * bắt nhịp tức thời khi vuốt nhanh.
 *
 *  • CAO ĐỘ dao động nhẹ (±6%) quanh 1.0 theo từng bước → chuỗi tick nghe TỰ NHIÊN như bánh răng thật,
 *    không lặp cứng máy móc.
 *  • Đi theo luồng SONIFICATION (≈ STREAM_SYSTEM) → tự TÔN TRỌNG chế độ im lặng/âm lượng hệ thống của máy;
 *    máy đang im lặng thì không kêu (giống rung vẫn chạy nhưng không phát tiếng).
 *
 * Vòng đời: tạo bằng [remember], nhớ gọi [release] trong onDispose khi rời màn để trả tài nguyên audio.
 */
class SliderTick(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6) // vuốt nhanh → nhiều tiếng chồng nhẹ, không cắt cụt tiếng trước
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    @Volatile private var loaded = false
    private val soundId = soundPool.load(context.applicationContext, R.raw.slider_tick, 1)
    private var step = 0 // đếm số tiếng đã phát → xoay cao độ mượt theo hình sin

    init {
        // load() là bất đồng bộ; chỉ phát sau khi nạp xong để tránh lỗi câm/streamID 0.
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId && status == 0) loaded = true
        }
    }

    /** Gọi mỗi khi thanh mục lục trượt sang CHỮ CÁI MỚI. Bỏ qua an toàn nếu mẫu chưa nạp xong. */
    fun tick() {
        if (!loaded) return
        step++
        val rate = 1f + 0.06f * sin(step * 1.7f) // ±6% quanh 1.0 → chất "quay bánh răng"
        soundPool.play(soundId, VOLUME, VOLUME, 1, 0, rate)
    }

    /** Trả tài nguyên SoundPool. Sau khi gọi thì instance không dùng lại được. */
    fun release() {
        soundPool.release()
    }

    private companion object {
        const val VOLUME = 0.9f
    }
}
