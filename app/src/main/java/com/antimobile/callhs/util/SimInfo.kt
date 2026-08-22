package com.antimobile.callhs.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Nhận diện các SIM đang lắp trên máy và suy ra NHÀ MẠNG của từng SIM — dùng để phân loại
 * nội/ngoại mạng khi tính cước. Đọc qua [SubscriptionManager], CẦN quyền READ_PHONE_STATE.
 *
 * KHÔNG ghi gì, chỉ đọc thông tin thuê bao đang hoạt động. Nếu chưa cấp quyền (hoặc máy không có
 * SIM) thì trả về danh sách RỖNG — nơi gọi tự hiểu là "chưa xác định" và không bịa dữ liệu.
 *
 * Lưu ý ghép với nhật ký cuộc gọi: cột PHONE_ACCOUNT_ID trong CallLog thường là subscriptionId
 * (dạng số) trên đa số máy, nhưng vài dòng máy lại lưu ICCID. Vì vậy [byAccountId] đánh khoá theo
 * CẢ hai để khớp bền hơn. Cuộc gọi cũ dùng SIM đã tháo sẽ không khớp SIM nào → nhà mạng = null.
 */
object SimInfo {

    /** Một SIM đang hoạt động. [carrier] đã chuẩn hoá về tên nhà mạng dùng chung ([Carrier]/[Tariffs]). */
    data class SimAccount(
        val subscriptionId: Int,
        val iccId: String?,
        val slotIndex: Int,
        val simLabel: String,        // "SIM 1" / "SIM 2" theo khe (slotIndex + 1)
        val carrier: String?,        // nhà mạng chuẩn hoá (Viettel/MobiFone/…); null nếu không rõ
        val displayName: String?,    // tên hiển thị của SIM (do nhà mạng/OEM đặt)
        val number: String?          // số thuê bao nếu đọc được (thường rỗng)
    )

    /** Danh sách SIM đang hoạt động; RỖNG nếu chưa cấp READ_PHONE_STATE hoặc không có SIM. */
    @Suppress("DEPRECATION") // SubscriptionInfo.number deprecated nhưng vẫn là 1 nguồn dự phòng hợp lệ khi có quyền
    fun activeSims(context: Context): List<SimAccount> {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return emptyList()
        val list: List<SubscriptionInfo> = try {
            sm.activeSubscriptionInfoList ?: emptyList()
        } catch (_: SecurityException) {
            return emptyList() // chưa có quyền
        } catch (_: Exception) {
            return emptyList()
        }
        return list.map { info ->
            val slot = info.simSlotIndex
            SimAccount(
                subscriptionId = info.subscriptionId,
                iccId = info.iccId?.takeIf { it.isNotBlank() },
                slotIndex = slot,
                simLabel = "SIM ${slot + 1}",
                carrier = normalizeCarrier(
                    name = sequenceOf(info.carrierName, info.displayName)
                        .firstOrNull { !it.isNullOrBlank() }?.toString(),
                    mcc = info.mccString,
                    mnc = info.mncString
                ),
                displayName = info.displayName?.toString()?.takeIf { it.isNotBlank() },
                number = runCatching { info.number }.getOrNull()?.takeIf { it.isNotBlank() }
            )
        }.sortedBy { it.slotIndex }
    }

    /**
     * ĐỌC TỰ ĐỘNG số thuê bao của một SIM, gộp nhiều nguồn (đa số SIM VN đều rỗng → trả ""):
     *  1. [SubscriptionManager.getPhoneNumber] (API 33+) — tổng hợp nguồn SIM/nhà mạng/IMS; CẦN READ_PHONE_NUMBERS.
     *  2. [quickNumber] = SubscriptionInfo.number đã đọc sẵn ở [activeSims].
     *  3. [TelephonyManager.getLine1Number] cho đúng subId (đã deprecated nhưng vẫn dùng được nếu có quyền).
     * Mọi lời gọi bọc runCatching để thiếu quyền/không hỗ trợ → bỏ qua, KHÔNG ném lỗi.
     */
    private fun readAutoNumber(context: Context, subscriptionId: Int, quickNumber: String?): String {
        // Both platform number APIs require a runtime phone-number permission. Keep this explicit
        // even though callers normally pass through onboarding: permissions can be revoked while
        // the process remains alive, in which case the cached SubscriptionInfo value is safest.
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) !=
            PackageManager.PERMISSION_GRANTED
        ) return quickNumber?.trim().orEmpty()

        val fromManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            runCatching { sm?.getPhoneNumber(subscriptionId) }.getOrNull()
        } else null
        val fromLine1 = runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            @Suppress("DEPRECATION")
            tm?.createForSubscriptionId(subscriptionId)?.line1Number
        }.getOrNull()
        return sequenceOf(fromManager, quickNumber, fromLine1)
            .firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }

    /**
     * Một KHE SIM để hiển thị trong màn "Số điện thoại của tôi" và "QR của tôi".
     * [autoNumber] = số đọc tự động ("" nếu không có); [manualNumber] = số người dùng tự nhập ("").
     */
    data class SlotInfo(
        val slotIndex: Int,          // 0 = SIM 1, 1 = SIM 2
        val label: String,           // "SIM 1" / "SIM 2"
        val present: Boolean,        // có SIM đang lắp ở khe này không
        val carrier: String?,        // nhà mạng chuẩn hoá (nếu present)
        val displayName: String?,    // tên hiển thị của SIM
        val autoNumber: String,      // số ĐỌC TỰ ĐỘNG được ("" nếu không)
        val manualNumber: String     // số NGƯỜI DÙNG TỰ NHẬP ("")
    ) {
        /** Số cuối cùng dùng để điền pattern / tạo QR: ưu tiên số tự đọc, thiếu thì lấy số nhập tay. */
        val resolved: String get() = autoNumber.ifBlank { manualNumber }

        /** Đọc tự động được số → màn Cài đặt hiển thị CHỈ XEM (không cho nhập tay). */
        val canAutoRead: Boolean get() = autoNumber.isNotBlank()

        /**
         * Nhà mạng DÙNG THỰC TẾ cho khe này — nguồn CHUNG cho mọi màn (Cước, Số của tôi):
         * ưu tiên nhà mạng SIM đang lắp; nếu không có SIM thì SUY từ đầu số người dùng đã nhập/đọc được
         * ([Carrier.of]). Null nếu cả hai đều không xác định được.
         */
        val effectiveCarrier: String? get() = carrier ?: Carrier.of(resolved)
    }

    /** Ảnh chụp SIM của máy để dựng UI: danh sách khe cần hiển thị + số SIM đang thực sự hoạt động. */
    data class DeviceSims(
        val slots: List<SlotInfo>,   // luôn có SIM 1; có SIM 2 khi máy đang lắp 2 SIM
        val activeCount: Int         // số SIM đang hoạt động (0/1/2)
    )

    /**
     * Đọc trạng thái SIM để dựng màn "Số điện thoại của tôi" + "QR của tôi".
     * QUY TẮC HIỂN THỊ KHE (theo yêu cầu): máy đang có 2 SIM → hiện SIM 1 + SIM 2; còn lại (1 SIM hoặc
     * KHÔNG có SIM) → chỉ hiện SIM 1. Số SIM 2 đã lưu trước đó (nếu người dùng tháo bớt SIM) VẪN nằm
     * trong kho, chỉ là không hiện ô nhập nữa; pattern {phonesim2} sẽ tự lùi về SIM 1 (xem TemplateFill).
     *
     * Ánh xạ THEO THỨ TỰ: SIM đang hoạt động thứ i (đã sort theo khe) → "SIM ${i+1}", số nhập tay khoá theo
     * i. Cách này bền hơn dùng chỉ số khe vật lý (tránh lệch nếu SIM duy nhất lại nằm ở khe 2) và khớp kỳ
     * vọng "1 SIM = SIM 1".
     */
    fun readDeviceSims(context: Context): DeviceSims {
        val sims = activeSims(context) // active, đã sort theo slotIndex
        val slotCount = if (sims.size >= 2) 2 else 1
        val slots = (0 until slotCount).map { idx ->
            val active = sims.getOrNull(idx)
            SlotInfo(
                slotIndex = idx,
                label = "SIM ${idx + 1}",
                present = active != null,
                carrier = active?.carrier,
                displayName = active?.displayName,
                autoNumber = active?.let { readAutoNumber(context, it.subscriptionId, it.number) }.orEmpty(),
                manualNumber = MyNumberStore.get(context, idx)
            )
        }
        return DeviceSims(slots = slots, activeCount = sims.size)
    }

    /** Số điện thoại của MÁY người dùng để điền pattern {phonesim1..N} + số SIM đang hoạt động (để ẩn/hiện {phonesim2}). */
    data class MyNumbers(
        val bySlot: List<String>,    // {phonesim1..N} theo thứ tự khe (index 0 = SIM 1); phần tử có thể rỗng
        val simCount: Int            // số SIM đang hoạt động → dùng để quyết định có hiện chip {phonesim2} hay không
    )

    /**
     * Số của máy để điền pattern trong mẫu tin nhắn. Mỗi khe: ưu tiên số ĐỌC TỰ ĐỘNG, thiếu thì lấy số
     * NGƯỜI DÙNG TỰ NHẬP. Có thể rỗng nếu cả hai đều trống — nơi gọi để trống, không bịa số.
     */
    fun myNumbers(context: Context): MyNumbers {
        val ds = readDeviceSims(context)
        return MyNumbers(bySlot = ds.slots.map { it.resolved }, simCount = ds.activeCount)
    }

    /** Map PHONE_ACCOUNT_ID → SIM, đánh khoá theo cả subscriptionId (dạng chuỗi) lẫn ICCID. */
    fun byAccountId(sims: List<SimAccount>): Map<String, SimAccount> {
        val map = HashMap<String, SimAccount>(sims.size * 2)
        for (s in sims) {
            map[s.subscriptionId.toString()] = s
            s.iccId?.let { map[it] = s }
        }
        return map
    }

    /**
     * Suy ra nhà mạng của SIM đã dùng cho một cuộc gọi.
     * - CÓ [phoneAccountId]: khớp SIM đang lắp → nhà mạng đó; KHÔNG khớp → null (chưa xác định).
     *   QUAN TRỌNG: không đoán theo SIM hiện tại cho cuộc gọi có id lạ — đó thường là cuộc gọi cũ dùng
     *   SIM đã tháo/đổi (có thể khác nhà mạng), đoán bừa sẽ tính SAI nội/ngoại mạng & sai cước.
     * - KHÔNG có id (máy 1 SIM thường để trống) & máy chỉ có ĐÚNG 1 SIM → mọi cuộc đều từ SIM đó.
     * - Còn lại → null (chưa xác định).
     */
    fun resolveCarrier(
        phoneAccountId: String?,
        byId: Map<String, SimAccount>,
        sims: List<SimAccount>
    ): String? {
        val id = phoneAccountId?.trim()?.takeIf { it.isNotBlank() }
        // Có id → chỉ chấp nhận khi khớp SIM đang lắp; id lạ (SIM đã tháo) → null, KHÔNG đoán bừa.
        if (id != null) return byId[id]?.carrier
        // Không có id & máy chỉ 1 SIM → không cần phân biệt tài khoản, mọi cuộc đi từ SIM duy nhất này.
        if (sims.size == 1) return sims[0].carrier
        return null
    }

    /**
     * Chuẩn hoá tên nhà mạng từ tên hiển thị của SIM (ưu tiên), rồi tới mã MCC+MNC (dự phòng).
     * Trả về tên chuẩn dùng chung với [Carrier]/[Tariffs], hoặc null nếu không nhận diện được.
     */
    fun normalizeCarrier(name: String?, mcc: String?, mnc: String?): String? {
        val n = name?.lowercase().orEmpty()
        when {
            // Mạng ảo (đặt trước để tên riêng không bị nuốt bởi tên mạng chủ).
            n.contains("itel") || n.contains("i-tel") || n.contains("i-telecom") || n.contains("indochina") -> return "iTel"
            n.contains("wintel") || n.contains("reddi") || n.contains("mobicast") -> return "Wintel"
            n.contains("vnsky") -> return "VNSKY"
            n.contains("fpt") -> return "FPT"
            // Nhà mạng chính.
            n.contains("viettel") -> return "Viettel"
            n.contains("mobifone") || n.contains("mobi fone") || n.contains("vms") -> return "MobiFone"
            n.contains("vinaphone") || n.contains("vina phone") || n.contains("vnpt") -> return "VinaPhone"
            n.contains("vietnamobile") -> return "Vietnamobile"
            n.contains("gmobile") || n.contains("gtel") || n.contains("beeline") -> return "Gmobile"
        }
        // Dự phòng theo mã mạng VN (MCC 452). MVNO dùng chung MNC với mạng chủ → chỉ ra được mạng chủ.
        if (mcc == "452") return when (mnc) {
            "01" -> "MobiFone"
            "02" -> "VinaPhone"
            "04", "06" -> "Viettel"
            "05" -> "Vietnamobile"
            "07" -> "Gmobile"
            else -> null
        }
        return null
    }
}
