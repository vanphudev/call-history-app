package com.antimobile.mcas

import com.antimobile.mcas.ui.permissions.resolvePermissionOnboardingPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionOnboardingStateTest {

    @Test
    fun freshInstall_followsBasePermissionsThenSmsThenConsent() {
        val first = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(false, false, false),
            shouldRequestSms = true,
            smsComplete = false,
            consented = false,
        )
        assertEquals(0, first.currentStageIndex)
        assertEquals(5, first.totalSteps)

        val sms = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, true, true),
            shouldRequestSms = true,
            smsComplete = false,
            consented = false,
        )
        assertEquals(3, sms.currentStageIndex)

        val consent = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, true, true),
            shouldRequestSms = true,
            smsComplete = true,
            consented = false,
        )
        assertEquals(4, consent.currentStageIndex)

        val complete = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, true, true),
            shouldRequestSms = true,
            smsComplete = true,
            consented = true,
        )
        assertEquals(-1, complete.currentStageIndex)
    }

    @Test
    fun partiallyGranted_stopsAtFirstMissingPermission() {
        val position = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, false, true),
            shouldRequestSms = true,
            smsComplete = false,
            consented = false,
        )

        assertEquals(1, position.currentPermissionIndex)
        assertEquals(1, position.currentStageIndex)
    }

    @Test
    fun unsupportedSms_skipsSmsAndKeepsConsentLast() {
        val position = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, true, true),
            shouldRequestSms = false,
            smsComplete = false,
            consented = false,
        )

        assertEquals(3, position.currentStageIndex)
        assertEquals(4, position.totalSteps)
    }

    @Test
    fun returningUser_doesNotRepeatAcceptedConsent() {
        val needsSms = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, true, true),
            shouldRequestSms = true,
            smsComplete = false,
            consented = true,
        )
        assertEquals(3, needsSms.currentStageIndex)
        assertEquals(4, needsSms.totalSteps)

        val complete = resolvePermissionOnboardingPosition(
            grantedPermissions = listOf(true, true, true),
            shouldRequestSms = true,
            smsComplete = true,
            consented = true,
        )
        assertEquals(-1, complete.currentStageIndex)
    }
}
