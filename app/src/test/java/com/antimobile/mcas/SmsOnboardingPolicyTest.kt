package com.antimobile.mcas

import com.antimobile.mcas.ui.permissions.SmsOnboardingPermissionState
import com.antimobile.mcas.ui.permissions.isSmsOnboardingComplete
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsOnboardingPolicyTest {

    @Test
    fun roleHeldAndCoreGranted_optionalMmsMissing_doesNotBlockOnboarding() {
        val state = SmsOnboardingPermissionState(
            roleHeld = true,
            corePermissionsGranted = true,
            optionalMmsPermissionsGranted = false,
        )

        assertTrue(isSmsOnboardingComplete(shouldRequestSms = true, permissionState = state))
        assertFalse(state.hasFullMessagingPermissions)
    }

    @Test
    fun roleMissing_blocksOnboardingEvenWhenEveryRuntimePermissionIsGranted() {
        val state = SmsOnboardingPermissionState(
            roleHeld = false,
            corePermissionsGranted = true,
            optionalMmsPermissionsGranted = true,
        )

        assertFalse(isSmsOnboardingComplete(shouldRequestSms = true, permissionState = state))
    }

    @Test
    fun corePermissionMissing_blocksOnboardingEvenWhenOptionalMmsIsGranted() {
        val state = SmsOnboardingPermissionState(
            roleHeld = true,
            corePermissionsGranted = false,
            optionalMmsPermissionsGranted = true,
        )

        assertFalse(isSmsOnboardingComplete(shouldRequestSms = true, permissionState = state))
    }

    @Test
    fun roleAndAllPermissionsGranted_completesWithFullMessagingAccess() {
        val state = SmsOnboardingPermissionState(
            roleHeld = true,
            corePermissionsGranted = true,
            optionalMmsPermissionsGranted = true,
        )

        assertTrue(isSmsOnboardingComplete(shouldRequestSms = true, permissionState = state))
        assertTrue(state.hasFullMessagingPermissions)
    }

    @Test
    fun unsupportedSms_skipsGateRegardlessOfPermissionState() {
        val state = SmsOnboardingPermissionState(
            roleHeld = false,
            corePermissionsGranted = false,
            optionalMmsPermissionsGranted = false,
        )

        assertTrue(isSmsOnboardingComplete(shouldRequestSms = false, permissionState = state))
    }
}
