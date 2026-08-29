package com.antimobile.mcas.util

import com.antimobile.mcas.data.model.CallEntry
import com.antimobile.mcas.data.model.CallType

/**
 * Ước tính cước cuộc gọi ĐI theo bảng giá cước PHỔ THÔNG (trả trước, ngoài gói) của các nhà mạng VN.
 *
 * QUAN TRỌNG — đây là ƯỚC TÍNH, không phải hoá đơn thật:
 * - Chỉ tính cho cuộc gọi ĐI ĐÃ kết nối (bên gọi trả tiền; cuộc gọi đến/nhỡ ở VN miễn phí).
 * - Giá dựa trên gói PHỔ THÔNG (không đăng ký gói cước). Nếu bạn đang dùng gói tháng có phút miễn
 *   phí thì cước thực tế thường THẤP hơn (nhiều khi bằng 0 trong hạn mức). Trả sau cũng rẻ hơn.
 * - Nội/ngoại mạng xác định bằng: nhà mạng SIM gọi đi (dò tự động) so với nhà mạng của số bị gọi
 *   (suy từ đầu số qua [Carrier]). Cùng nhà mạng = nội mạng; khác = ngoại mạng; số lạ/cố định = "Khác".
 *
 * Bảng giá & cách tính block tra cứu 07/2026 (đã đối chiếu nhiều nguồn, gồm trang chính thức nhà mạng
 * và đại lý viễn thông). Đơn vị: đồng/phút, đã gồm VAT.
 *  - Viettel     1.590 / 1.790  (gói Tomato phổ thông; gói Economy rẻ hơn 1.190/1.390)   block 6s+1
 *  - MobiFone    1.180 / 1.380  (MobiCard)                                               block 6s+1
 *  - VinaPhone   1.180 / 1.380  (VinaCard; gọi số cố định VNPT tính như nội mạng)        block 6s+1
 *  - Vietnamobile  680 /   990                                                           block 6s+1
 *  - Gmobile     1.180 / 1.380  (mạng gần như ngừng hoạt động — giá cũ)                  block 6s+1
 *  - iTel          750 / 1.100  (MVNO trên VinaPhone, tự công bố giá riêng)              block 6s+1
 *  - Wintel      1.500 / 1.500  (MVNO trên VinaPhone; 1 giá phẳng)                       block 60s+1
 *  - VNSKY         690 /   690  (MVNO trên MobiFone; 1 giá phẳng, trong vùng)            tính tròn phút
 *  - FPT           690 /   690  (MVNO trên MobiFone; 1 giá phẳng, trong vùng)            block 6s+1
 * Nguồn: viettelnet.vn, mobifone.net.vn, digishop.vnpt.vn, itel.vn, gmobile.vn/go, wintel.vn, vnsky.vn,
 * fptshop.com.vn/ho-tro/chinh-sach-gia-cuoc, muathe6s.com, hanoimoi.vn (block 6s+1).
 */

/** Cách tính thời lượng tính cước (block) của nhà mạng. */
enum class BillingBlock {
    /** Block 6 giây + 1: 6 giây đầu tính trọn 1 block, sau đó tính từng giây. (Chuẩn phổ biến VN.) */
    SIX_PLUS_ONE,

    /** Block 60 giây + 1: 60 giây đầu tính trọn 1 phút, sau đó tính từng giây. (Wintel.) */
    SIXTY_PLUS_ONE,

    /** Tính tròn phút: mọi cuộc làm tròn LÊN phút. (VNSKY.) */
    PER_MINUTE;

    /**
     * Số giây TÍNH CƯỚC cho [durationSeconds] giây đàm thoại thực tế theo block này.
     * 0 nếu không đàm thoại.
     */
    fun billedSeconds(durationSeconds: Long): Long {
        if (durationSeconds <= 0) return 0
        return when (this) {
            SIX_PLUS_ONE -> maxOf(durationSeconds, 6L)
            SIXTY_PLUS_ONE -> if (durationSeconds <= 60L) 60L else durationSeconds
            PER_MINUTE -> ((durationSeconds + 59L) / 60L) * 60L // làm tròn lên bội số 60
        }
    }
}

/** Phân loại cuộc gọi đi theo mạng đích để chọn đơn giá. */
enum class NetworkClass {
    /** Cùng nhà mạng với SIM gọi đi. */
    NOI_MANG,

    /** Sang nhà mạng di động khác. */
    NGOAI_MANG,

    /** Số cố định / quốc tế / đầu số lạ không nhận diện được nhà mạng — tạm tính theo giá ngoại mạng. */
    KHAC,

    /** Không áp dụng (cuộc gọi miễn phí / chưa xác định). */
    NONE
    // Nhãn hiển thị (Nội mạng/Ngoại mạng/…) resolve ở tầng UI theo ngôn ngữ — xem CostStatsScreen.
}

/** Tình trạng tính cước của một cuộc gọi. */
enum class ChargeStatus {
    /** Đã tính ra cước. */
    CHARGED,

    /** Cuộc gọi đến / nhỡ — miễn phí với người dùng. */
    FREE_INCOMING,

    /** Gọi đi nhưng không kết nối — không phát sinh cước. */
    FREE_NO_CONNECT,

    /** Gọi đi đã kết nối nhưng chưa biết nhà mạng SIM (chưa cấp quyền / SIM đã tháo) → chưa tính được. */
    UNKNOWN_SIM
}

/** Bảng giá của một nhà mạng: đơn giá nội/ngoại mạng (đồng/phút) + cách tính block. */
data class Tariff(
    val noiMangPerMinute: Int,
    val ngoaiMangPerMinute: Int,
    val block: BillingBlock
)

object Tariffs {

    /** Bảng giá phổ thông theo nhà mạng (khoá = tên nhà mạng chuẩn, trùng với [Carrier] & [SimInfo]). */
    val table: Map<String, Tariff> = mapOf(
        "Viettel" to Tariff(1590, 1790, BillingBlock.SIX_PLUS_ONE),
        "MobiFone" to Tariff(1180, 1380, BillingBlock.SIX_PLUS_ONE),
        "VinaPhone" to Tariff(1180, 1380, BillingBlock.SIX_PLUS_ONE),
        "Vietnamobile" to Tariff(680, 990, BillingBlock.SIX_PLUS_ONE),
        "Gmobile" to Tariff(1180, 1380, BillingBlock.SIX_PLUS_ONE),
        "iTel" to Tariff(750, 1100, BillingBlock.SIX_PLUS_ONE),
        "Wintel" to Tariff(1500, 1500, BillingBlock.SIXTY_PLUS_ONE),
        "VNSKY" to Tariff(690, 690, BillingBlock.PER_MINUTE),
        "FPT" to Tariff(690, 690, BillingBlock.SIX_PLUS_ONE)
    )

    /** Giá dùng khi biết nhà mạng SIM nhưng không có trong bảng (hiếm) — lấy mức phổ biến MobiCard. */
    val fallback = Tariff(1180, 1380, BillingBlock.SIX_PLUS_ONE)

    fun of(carrier: String?): Tariff = carrier?.let { table[it] } ?: fallback
}

/**
 * Kết quả tính cước cho MỘT cuộc gọi.
 * [chargeable] = đã tính ra được cước ([status] == [ChargeStatus.CHARGED]).
 */
data class CallCharge(
    val entry: CallEntry,
    val status: ChargeStatus,
    val simCarrier: String?,       // nhà mạng SIM gọi đi (null = chưa xác định)
    val calledCarrier: String?,    // nhà mạng của số bị gọi (suy từ đầu số)
    val networkClass: NetworkClass,
    val billedSeconds: Long,       // số giây tính cước theo block
    val ratePerMinute: Int,        // đơn giá áp dụng (đồng/phút)
    val block: BillingBlock,
    val cost: Long                 // thành tiền (đồng), đã làm tròn
) {
    val chargeable: Boolean get() = status == ChargeStatus.CHARGED
}

/** Tổng hợp cước cho toàn bộ lịch sử với một số. */
data class CostSummary(
    val totalCost: Long,
    val chargeableCalls: Int,      // số cuộc tính được phí
    val billedSeconds: Long,       // tổng giây tính cước
    val noiMangCalls: Int, val noiMangCost: Long,
    val ngoaiMangCalls: Int, val ngoaiMangCost: Long,
    val khacCalls: Int, val khacCost: Long,
    val freeCalls: Int,            // cuộc gọi đến / nhỡ / gọi đi không kết nối → miễn phí
    val unknownSimCalls: Int       // cuộc gọi đi đã kết nối nhưng chưa biết nhà mạng SIM
)

object CostCalculator {

    /**
     * Tính cước cho một cuộc gọi.
     * @param simCarrier nhà mạng của SIM đã dùng gọi (dò từ [SimInfo]); null nếu chưa xác định.
     */
    fun charge(entry: CallEntry, simCarrier: String?): CallCharge {
        // Chỉ cuộc gọi ĐI mới tốn tiền của người dùng.
        if (entry.type != CallType.OUTGOING) {
            return free(entry, simCarrier, ChargeStatus.FREE_INCOMING)
        }
        // Gọi đi nhưng không đàm thoại (không nghe máy / máy bận…) → không phát sinh cước.
        if (!CallResults.isConnected(entry.type, entry.durationSeconds)) {
            return free(entry, simCarrier, ChargeStatus.FREE_NO_CONNECT)
        }
        // Gọi đi đã kết nối nhưng chưa biết nhà mạng SIM → không thể tính nội/ngoại mạng.
        if (simCarrier == null) {
            return CallCharge(
                entry = entry,
                status = ChargeStatus.UNKNOWN_SIM,
                simCarrier = null,
                calledCarrier = entry.carrier,
                networkClass = NetworkClass.NONE,
                billedSeconds = 0L,
                ratePerMinute = 0,
                block = BillingBlock.SIX_PLUS_ONE,
                cost = 0L
            )
        }

        val calledCarrier = entry.carrier
        val networkClass = when {
            calledCarrier == null -> NetworkClass.KHAC
            calledCarrier == simCarrier -> NetworkClass.NOI_MANG
            else -> NetworkClass.NGOAI_MANG
        }
        val tariff = Tariffs.of(simCarrier)
        val ratePerMinute = when (networkClass) {
            NetworkClass.NOI_MANG -> tariff.noiMangPerMinute
            else -> tariff.ngoaiMangPerMinute // NGOAI_MANG & KHAC tạm tính theo giá ngoại mạng
        }
        val billed = tariff.block.billedSeconds(entry.durationSeconds)
        val cost = Math.round(ratePerMinute.toDouble() * billed / 60.0)
        return CallCharge(
            entry = entry,
            status = ChargeStatus.CHARGED,
            simCarrier = simCarrier,
            calledCarrier = calledCarrier,
            networkClass = networkClass,
            billedSeconds = billed,
            ratePerMinute = ratePerMinute,
            block = tariff.block,
            cost = cost
        )
    }

    private fun free(entry: CallEntry, simCarrier: String?, status: ChargeStatus) = CallCharge(
        entry = entry,
        status = status,
        simCarrier = simCarrier,
        calledCarrier = entry.carrier,
        networkClass = NetworkClass.NONE,
        billedSeconds = 0L,
        ratePerMinute = 0,
        block = BillingBlock.SIX_PLUS_ONE,
        cost = 0L
    )

    fun summarize(charges: List<CallCharge>): CostSummary {
        var total = 0L
        var chargeable = 0
        var billed = 0L
        var noiN = 0; var noiC = 0L
        var ngoaiN = 0; var ngoaiC = 0L
        var khacN = 0; var khacC = 0L
        var free = 0
        var unknown = 0
        for (c in charges) {
            when (c.status) {
                ChargeStatus.CHARGED -> {
                    total += c.cost
                    chargeable++
                    billed += c.billedSeconds
                    when (c.networkClass) {
                        NetworkClass.NOI_MANG -> { noiN++; noiC += c.cost }
                        NetworkClass.NGOAI_MANG -> { ngoaiN++; ngoaiC += c.cost }
                        NetworkClass.KHAC -> { khacN++; khacC += c.cost }
                        NetworkClass.NONE -> {}
                    }
                }
                ChargeStatus.UNKNOWN_SIM -> unknown++
                ChargeStatus.FREE_INCOMING, ChargeStatus.FREE_NO_CONNECT -> free++
            }
        }
        return CostSummary(
            totalCost = total,
            chargeableCalls = chargeable,
            billedSeconds = billed,
            noiMangCalls = noiN, noiMangCost = noiC,
            ngoaiMangCalls = ngoaiN, ngoaiMangCost = ngoaiC,
            khacCalls = khacN, khacCost = khacC,
            freeCalls = free,
            unknownSimCalls = unknown
        )
    }
}
