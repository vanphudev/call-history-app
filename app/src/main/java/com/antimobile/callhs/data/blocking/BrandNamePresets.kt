package com.antimobile.callhs.data.blocking

enum class BrandNamePresetCategory {
    TELECOM,
    BANK,
    E_WALLET,
    SERVICE,
    SPAM_WARNING,
}

data class BrandNamePresetGroup(
    val category: BrandNamePresetCategory,
    val names: List<String>,
)

/**
 * Short, editable suggestions for exact caller-ID labels. These are not an authoritative registry:
 * carriers and caller-ID apps may display different casing or localized warning text.
 */
object BrandNamePresetCatalog {
    val groups: List<BrandNamePresetGroup> = listOf(
        BrandNamePresetGroup(
            BrandNamePresetCategory.TELECOM,
            listOf("VIETTELCSKH", "VinaPhone", "MobiFone", "VNPT", "LOCAL"),
        ),
        BrandNamePresetGroup(
            BrandNamePresetCategory.BANK,
            listOf("Vietcombank", "BIDV", "VietinBank", "Agribank", "VPBank"),
        ),
        BrandNamePresetGroup(
            BrandNamePresetCategory.E_WALLET,
            listOf("MoMo", "ZaloPay", "ShopeePay", "VNPAY", "Viettel Money"),
        ),
        BrandNamePresetGroup(
            BrandNamePresetCategory.SERVICE,
            listOf("BO TTTT", "FPT SHOP", "Grab", "Shopee", "Lazada"),
        ),
        BrandNamePresetGroup(
            BrandNamePresetCategory.SPAM_WARNING,
            listOf("Spam", "SPAM", "Suspected spam caller", "Cuộc gọi rác", "Nghi ngờ lừa đảo"),
        ),
    )

    init {
        require(groups.all { it.names.size <= BrandNameRuleCodec.MAX_NAMES })
        require(groups.flatMap { it.names }.all(BrandNameRuleCodec::isAllowedName))
    }
}
