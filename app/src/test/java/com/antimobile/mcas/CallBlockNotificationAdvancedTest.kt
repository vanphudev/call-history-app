package com.antimobile.mcas

import com.antimobile.mcas.data.blocking.BlockNotificationAdvancedConfig
import com.antimobile.mcas.data.blocking.BlockNotificationAlert
import com.antimobile.mcas.data.blocking.BlockNotificationPeriod
import com.antimobile.mcas.data.blocking.BlockNotificationPeriodSettings
import com.antimobile.mcas.data.blocking.BlockNotificationPresentation
import com.antimobile.mcas.data.blocking.BlockNotificationSound
import com.antimobile.mcas.data.blocking.BlockNotificationSoundPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockNotificationAdvancedTest {

    @Test
    fun freshScheduleStartsWithAllFourPeriodsDisabled() {
        val periods = BlockNotificationAdvancedConfig.defaultPeriods()

        assertEquals(4, periods.size)
        assertTrue(periods.none { it.enabled })
    }

    @Test
    fun headsUpIsTheBackwardCompatibleDefaultPresentation() {
        assertEquals(
            BlockNotificationPresentation.HEADS_UP,
            BlockNotificationAlert().presentation,
        )
    }

    @Test
    fun fourPeriodsCoverTheWholeDayWithoutOverlappingAtBoundaries() {
        for (minute in 0 until 24 * 60) {
            assertEquals(1, BlockNotificationPeriod.entries.count { it.contains(minute) })
        }
        assertTrue(BlockNotificationPeriod.NIGHT.contains(0))
        assertTrue(BlockNotificationPeriod.MORNING.contains(6 * 60))
        assertTrue(BlockNotificationPeriod.AFTERNOON.contains(12 * 60))
        assertTrue(BlockNotificationPeriod.EVENING.contains(18 * 60))
        assertTrue(BlockNotificationPeriod.NIGHT.contains(22 * 60))
        assertFalse(BlockNotificationPeriod.MORNING.contains(12 * 60))
    }

    @Test
    fun defaultConfigurationIsUsedOnlyWhileScheduleIsOff() {
        val default = BlockNotificationAlert(
            soundEnabled = false,
            vibrationEnabled = true,
            sound = BlockNotificationSound.preset(BlockNotificationSoundPreset.CRYSTAL),
        )
        val morning = BlockNotificationAlert(
            soundEnabled = true,
            vibrationEnabled = false,
            sound = BlockNotificationSound.preset(BlockNotificationSoundPreset.BAMBOO),
        )
        val periods = BlockNotificationAdvancedConfig.defaultPeriods().map {
            if (it.period == BlockNotificationPeriod.MORNING) {
                it.copy(enabled = true, alert = morning)
            } else {
                it
            }
        }

        assertEquals(default, BlockNotificationAdvancedConfig(defaultAlert = default).alertAt(8 * 60))
        assertEquals(
            morning,
            BlockNotificationAdvancedConfig(
                defaultAlert = default,
                scheduleEnabled = true,
                periods = periods,
            ).alertAt(8 * 60),
        )
    }

    @Test
    fun disabledScheduledPeriodSuppressesNotificationInsteadOfFallingBackToDefault() {
        val periods = BlockNotificationAdvancedConfig.defaultPeriods().map {
            if (it.period == BlockNotificationPeriod.NIGHT) it.copy(enabled = false) else it
        }
        val config = BlockNotificationAdvancedConfig(
            defaultAlert = BlockNotificationAlert(soundEnabled = true, vibrationEnabled = true),
            scheduleEnabled = true,
            periods = periods,
        )

        assertNull(config.alertAt(23 * 60))
        assertNull(config.alertAt(3 * 60))
    }

    @Test
    fun normalizationAlwaysRestoresExactlyFourNamedPeriods() {
        val config = BlockNotificationAdvancedConfig(
            scheduleEnabled = true,
            periods = listOf(
                BlockNotificationPeriodSettings(BlockNotificationPeriod.MORNING, enabled = false),
                BlockNotificationPeriodSettings(BlockNotificationPeriod.MORNING, enabled = true),
            ),
        ).normalized()

        assertEquals(BlockNotificationPeriod.entries, config.periods.map { it.period })
        assertEquals(4, config.periods.size)
    }
}
