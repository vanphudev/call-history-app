package com.antimobile.callhs

import com.antimobile.callhs.data.outgoing.OutgoingCallAlertPolicy
import com.antimobile.callhs.data.outgoing.OutgoingCallAlertReason
import com.antimobile.callhs.data.outgoing.OutgoingCallConfig
import com.antimobile.callhs.data.outgoing.OutgoingCallPresentation
import com.antimobile.callhs.data.outgoing.OutgoingNumberList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutgoingCallAlertPolicyTest {
    private fun config(
        enabled: Boolean = true,
        offNetwork: Boolean = true,
        blocklist: Boolean = true,
        allowlist: Boolean = true,
    ) = OutgoingCallConfig(
        enabled = enabled,
        notifyOffNetwork = offNetwork,
        notifyBlocklist = blocklist,
        notifyAllowlist = allowlist,
        presentation = OutgoingCallPresentation.HEADS_UP,
    )

    @Test
    fun disabledFeatureNeverAlerts() {
        val decision = OutgoingCallAlertPolicy.evaluate(
            config(enabled = false),
            membership = OutgoingNumberList.BLOCKLIST,
            simCarrier = "Viettel",
            targetCarrier = "MobiFone",
        )

        assertFalse(decision.shouldAlert)
    }

    @Test
    fun offNetworkRequiresTwoKnownDifferentCarriers() {
        assertTrue(
            OutgoingCallAlertPolicy.evaluate(
                config(), OutgoingNumberList.NONE, "Viettel", "MobiFone",
            ).shouldAlert,
        )
        assertFalse(
            OutgoingCallAlertPolicy.evaluate(
                config(), OutgoingNumberList.NONE, "Viettel", "Viettel",
            ).shouldAlert,
        )
        assertFalse(
            OutgoingCallAlertPolicy.evaluate(
                config(), OutgoingNumberList.NONE, null, "Viettel",
            ).shouldAlert,
        )
        assertFalse(
            OutgoingCallAlertPolicy.evaluate(
                config(), OutgoingNumberList.NONE, "Viettel", null,
            ).shouldAlert,
        )
    }

    @Test
    fun blocklistAndOffNetworkReasonsAreCombinedInWarningOrder() {
        val decision = OutgoingCallAlertPolicy.evaluate(
            config(),
            membership = OutgoingNumberList.BLOCKLIST,
            simCarrier = "Viettel",
            targetCarrier = "MobiFone",
        )

        assertEquals(
            listOf(
                OutgoingCallAlertReason.BLOCKLIST,
                OutgoingCallAlertReason.OFF_NETWORK,
            ),
            decision.reasons,
        )
    }

    @Test
    fun eachListConditionRespectsItsOwnToggle() {
        val allow = OutgoingCallAlertPolicy.evaluate(
            config(offNetwork = false, blocklist = false, allowlist = true),
            membership = OutgoingNumberList.ALLOWLIST,
            simCarrier = null,
            targetCarrier = null,
        )
        val blockDisabled = OutgoingCallAlertPolicy.evaluate(
            config(offNetwork = false, blocklist = false, allowlist = true),
            membership = OutgoingNumberList.BLOCKLIST,
            simCarrier = null,
            targetCarrier = null,
        )

        assertEquals(listOf(OutgoingCallAlertReason.ALLOWLIST), allow.reasons)
        assertFalse(blockDisabled.shouldAlert)
    }
}
