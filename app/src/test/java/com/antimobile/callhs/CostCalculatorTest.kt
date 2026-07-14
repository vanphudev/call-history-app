package com.antimobile.callhs

import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.util.BillingBlock
import com.antimobile.callhs.util.ChargeStatus
import com.antimobile.callhs.util.CostCalculator
import com.antimobile.callhs.util.NetworkClass
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.formatDong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Kiểm thử LOGIC thuần (không phụ thuộc Android) cho tính cước cuộc gọi. */
class CostCalculatorTest {

    private fun call(
        dur: Long,
        calledCarrier: String?,
        type: CallType = CallType.OUTGOING,
        accountId: String? = "1"
    ) = CallEntry(
        id = 1L, number = "0900000000", cachedName = null, type = type, dateMillis = 0L,
        durationSeconds = dur, photoUri = null, geocodedLocation = null, numberType = 0, numberTypeCustom = null,
        phoneAccountId = accountId, simLabel = "SIM 1", carrier = calledCarrier, isVideo = false, isVolte = false
    )

    // ---- Block billed-seconds ----

    @Test fun block6s1_roundsShortCallsUpToSixSeconds() {
        assertEquals(0L, BillingBlock.SIX_PLUS_ONE.billedSeconds(0))
        assertEquals(6L, BillingBlock.SIX_PLUS_ONE.billedSeconds(1))
        assertEquals(6L, BillingBlock.SIX_PLUS_ONE.billedSeconds(6))
        assertEquals(7L, BillingBlock.SIX_PLUS_ONE.billedSeconds(7))
        assertEquals(65L, BillingBlock.SIX_PLUS_ONE.billedSeconds(65))
    }

    @Test fun block60s1_billsFirstMinuteWhole() {
        assertEquals(60L, BillingBlock.SIXTY_PLUS_ONE.billedSeconds(1))
        assertEquals(60L, BillingBlock.SIXTY_PLUS_ONE.billedSeconds(60))
        assertEquals(61L, BillingBlock.SIXTY_PLUS_ONE.billedSeconds(61))
        assertEquals(120L, BillingBlock.SIXTY_PLUS_ONE.billedSeconds(120))
    }

    @Test fun perMinute_roundsUpToWholeMinutes() {
        assertEquals(60L, BillingBlock.PER_MINUTE.billedSeconds(1))
        assertEquals(60L, BillingBlock.PER_MINUTE.billedSeconds(60))
        assertEquals(120L, BillingBlock.PER_MINUTE.billedSeconds(61))
        assertEquals(120L, BillingBlock.PER_MINUTE.billedSeconds(120))
    }

    // ---- Phân loại + đơn giá ----

    @Test fun noiMang_usesIntraRate() {
        val c = CostCalculator.charge(call(60, "Viettel"), simCarrier = "Viettel")
        assertEquals(ChargeStatus.CHARGED, c.status)
        assertEquals(NetworkClass.NOI_MANG, c.networkClass)
        assertEquals(1590L, c.cost) // 1590đ/phút · 60s
    }

    @Test fun ngoaiMang_usesInterRate() {
        val c = CostCalculator.charge(call(60, "MobiFone"), simCarrier = "Viettel")
        assertEquals(NetworkClass.NGOAI_MANG, c.networkClass)
        assertEquals(1790L, c.cost) // Viettel ngoại 1790đ/phút · 60s
    }

    @Test fun shortNoiMangCall_billedAsSixSecondBlock() {
        val c = CostCalculator.charge(call(3, "Viettel"), simCarrier = "Viettel")
        assertEquals(6L, c.billedSeconds)
        assertEquals(159L, c.cost) // round(1590 * 6 / 60)
    }

    @Test fun unknownDialedCarrier_isKhac_pricedAtInterRate() {
        val c = CostCalculator.charge(call(60, null), simCarrier = "MobiFone")
        assertEquals(NetworkClass.KHAC, c.networkClass)
        assertEquals(1380L, c.cost) // MobiFone ngoại 1380
    }

    @Test fun wintel_flatRate_sixtySecondBlock() {
        val c = CostCalculator.charge(call(30, "Viettel"), simCarrier = "Wintel")
        assertEquals(60L, c.billedSeconds) // 30s → tính trọn 60s
        assertEquals(1500L, c.cost)
    }

    @Test fun vnsky_perMinuteRounding() {
        val c = CostCalculator.charge(call(61, "VNSKY"), simCarrier = "VNSKY")
        assertEquals(NetworkClass.NOI_MANG, c.networkClass)
        assertEquals(120L, c.billedSeconds) // 61s → 2 phút
        assertEquals(1380L, c.cost) // round(690 * 120 / 60)
    }

    // ---- Miễn phí / chưa xác định ----

    @Test fun incomingCall_isFree() {
        val c = CostCalculator.charge(call(120, "Viettel", type = CallType.INCOMING), simCarrier = "Viettel")
        assertEquals(ChargeStatus.FREE_INCOMING, c.status)
        assertEquals(0L, c.cost)
    }

    @Test fun outgoingNotConnected_isFree() {
        val c = CostCalculator.charge(call(0, "Viettel"), simCarrier = "Viettel")
        assertEquals(ChargeStatus.FREE_NO_CONNECT, c.status)
        assertEquals(0L, c.cost)
    }

    @Test fun outgoingConnectedButUnknownSim_isNotCharged() {
        val c = CostCalculator.charge(call(60, "Viettel"), simCarrier = null)
        assertEquals(ChargeStatus.UNKNOWN_SIM, c.status)
        assertEquals(0L, c.cost)
    }

    // ---- Tổng hợp ----

    @Test fun summarize_bucketsAndTotals() {
        val charges = listOf(
            CostCalculator.charge(call(60, "Viettel"), "Viettel"),   // nội 1590
            CostCalculator.charge(call(60, "MobiFone"), "Viettel"),  // ngoại 1790
            CostCalculator.charge(call(60, null), "Viettel"),        // khác 1790
            CostCalculator.charge(call(30, "Viettel", type = CallType.INCOMING), "Viettel"), // free
            CostCalculator.charge(call(60, "Viettel"), null)         // unknown sim
        )
        val s = CostCalculator.summarize(charges)
        assertEquals(3, s.chargeableCalls)
        assertEquals(1590L + 1790L + 1790L, s.totalCost)
        assertEquals(1, s.noiMangCalls)
        assertEquals(1, s.ngoaiMangCalls)
        assertEquals(1, s.khacCalls)
        assertEquals(1, s.freeCalls)
        assertEquals(1, s.unknownSimCalls)
    }

    // ---- resolveCarrier: KHÔNG đoán bừa với id lạ (bản vá lỗi) ----

    @Test fun resolveCarrier_presentUnmatchedId_returnsNull_evenWithSingleSim() {
        val sim = SimInfo.SimAccount(3, "ICC3", 0, "SIM 1", "MobiFone", "MobiFone", null)
        val byId = SimInfo.byAccountId(listOf(sim))
        // id "1" (SIM cũ đã tháo) không khớp SIM đang lắp "3" → null, KHÔNG trả về MobiFone.
        assertNull(SimInfo.resolveCarrier("1", byId, listOf(sim)))
        // id khớp → nhà mạng của SIM đó.
        assertEquals("MobiFone", SimInfo.resolveCarrier("3", byId, listOf(sim)))
        // Không có id + đúng 1 SIM → SIM duy nhất đó.
        assertEquals("MobiFone", SimInfo.resolveCarrier(null, byId, listOf(sim)))
    }

    @Test fun resolveCarrier_multiSim_matchesById() {
        val viet = SimInfo.SimAccount(1, "ICC1", 0, "SIM 1", "Viettel", "Viettel", null)
        val mobi = SimInfo.SimAccount(2, "ICC2", 1, "SIM 2", "MobiFone", "MobiFone", null)
        val sims = listOf(viet, mobi)
        val byId = SimInfo.byAccountId(sims)
        assertEquals("Viettel", SimInfo.resolveCarrier("1", byId, sims))
        assertEquals("MobiFone", SimInfo.resolveCarrier("2", byId, sims))
        assertNull(SimInfo.resolveCarrier("99", byId, sims)) // id lạ, nhiều SIM → null
    }

    // ---- normalizeCarrier ----

    @Test fun normalizeCarrier_byNameAndMnc() {
        assertEquals("iTel", SimInfo.normalizeCarrier("I-Telecom", "452", "02"))
        assertEquals("Viettel", SimInfo.normalizeCarrier("Viettel Mobile", null, null))
        assertEquals("MobiFone", SimInfo.normalizeCarrier(null, "452", "01"))
        assertEquals("VinaPhone", SimInfo.normalizeCarrier(null, "452", "02"))
        assertNull(SimInfo.normalizeCarrier(null, "310", "260")) // mạng nước ngoài
    }

    // ---- Định dạng tiền ----

    @Test fun formatDong_groupsThousandsWithDots() {
        assertEquals("0 đ", formatDong(0))
        assertEquals("1.590 đ", formatDong(1590))
        assertEquals("1.234.567 đ", formatDong(1234567))
    }
}
