package com.antimobile.mcas

import com.antimobile.mcas.data.backup.BackupOutgoingCallConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupOutgoingCallConfigTest {
    @Test
    fun anyOutgoingSettingMakesSectionRestorable() {
        assertTrue(
            BackupOutgoingCallConfig(
                enabled = null,
                notifyOffNetwork = null,
                notifyBlocklist = true,
                notifyAllowlist = null,
                presentation = null,
            ).hasAny,
        )
    }

    @Test
    fun completelyMissingOutgoingConfigIsEmpty() {
        assertFalse(
            BackupOutgoingCallConfig(
                enabled = null,
                notifyOffNetwork = null,
                notifyBlocklist = null,
                notifyAllowlist = null,
                presentation = null,
            ).hasAny,
        )
    }
}
