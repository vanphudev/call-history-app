package com.antimobile.mcas.ui.donate

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.donate.BankApp
import com.antimobile.mcas.data.donate.BankAppRepository
import com.antimobile.mcas.ui.components.RemoteImageLoader
import com.antimobile.mcas.util.Donate
import kotlinx.coroutines.launch

/**
 * State cho màn ỦNG HỘ: số tiền đang chọn + danh sách app ngân hàng. Giữ ở ViewModel để sống qua xoay màn
 * & không tải lại. Khi khởi tạo: TẢI SẴN ảnh QR mọi mức gợi ý (đổi số tiền là hiện tức thì) và nạp danh
 * sách app ngân hàng (hiện tạm [BankApp.BUNDLED] rồi nâng cấp bằng danh sách đầy đủ kèm logo).
 */
class DonateViewModel(app: Application) : AndroidViewModel(app) {

    /** Số tiền đang chọn (VND); 0 = "tuỳ tâm" (mã QR mở). Mặc định 10.000đ. */
    var selectedAmount by mutableStateOf(10_000L)
        private set

    var bankApps by mutableStateOf(BankApp.BUNDLED)
        private set

    init {
        viewModelScope.launch {
            (Donate.PRESET_AMOUNTS + 0L).forEach {
                RemoteImageLoader.prefetch(getApplication(), Donate.imageUrl(it))
            }
        }
        viewModelScope.launch {
            bankApps = BankAppRepository.load(getApplication())
        }
    }

    /** Chọn số tiền (âm → 0 = mã mở). Tải sẵn ảnh QR mức mới nếu chưa có (an toàn khi nhập số khác). */
    fun select(amount: Long) {
        val a = amount.coerceAtLeast(0L)
        selectedAmount = a
        viewModelScope.launch { RemoteImageLoader.prefetch(getApplication(), Donate.imageUrl(a)) }
    }
}
