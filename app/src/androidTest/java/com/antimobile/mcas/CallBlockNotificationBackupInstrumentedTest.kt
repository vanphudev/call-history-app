package com.antimobile.mcas

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antimobile.mcas.data.backup.BackupManager
import com.antimobile.mcas.data.backup.BackupSection
import com.antimobile.mcas.data.backup.MergeMode
import com.antimobile.mcas.data.blocking.BlockNotificationAdvancedConfig
import com.antimobile.mcas.data.blocking.BlockNotificationAlert
import com.antimobile.mcas.data.blocking.BlockNotificationPeriod
import com.antimobile.mcas.data.blocking.BlockNotificationPeriodSettings
import com.antimobile.mcas.data.blocking.BlockNotificationPresentation
import com.antimobile.mcas.data.blocking.BlockNotificationSound
import com.antimobile.mcas.data.blocking.BlockNotificationSoundPreset
import com.antimobile.mcas.data.blocking.CallBlockNotificationSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallBlockNotificationBackupInstrumentedTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun exportParseAndRestorePreservesPortableAdvancedNotificationSettings() {
        val original = CallBlockNotificationSettings.read(context)
        val backedUp = BlockNotificationAdvancedConfig(
            defaultAlert = alert(
                soundEnabled = false,
                vibrationEnabled = true,
                preset = BlockNotificationSoundPreset.CRYSTAL,
                presentation = BlockNotificationPresentation.STATUS_BAR,
            ),
            scheduleEnabled = true,
            periods = BlockNotificationPeriod.entries.mapIndexed { index, period ->
                BlockNotificationPeriodSettings(
                    period = period,
                    enabled = index % 2 == 0,
                    alert = alert(
                        soundEnabled = index != 1,
                        vibrationEnabled = index != 2,
                        preset = BlockNotificationSoundPreset.entries[index],
                        presentation = if (index % 2 == 0) {
                            BlockNotificationPresentation.HEADS_UP
                        } else {
                            BlockNotificationPresentation.STATUS_BAR
                        },
                    ),
                )
            },
        )

        try {
            assertTrue(CallBlockNotificationSettings.replace(context, backedUp))
            val json = runBlocking {
                BackupManager.buildJson(context, setOf(BackupSection.BLOCK_RULES))
            }
            val root = JSONObject(json)
            assertEquals(6, root.getInt("version"))
            val advanced = root.getJSONObject("sections")
                .getJSONObject("callBlockRules")
                .getJSONObject("advancedNotification")
            assertTrue(advanced.getBoolean("scheduleEnabled"))
            assertEquals("crystal", advanced.getJSONObject("default").getString("soundPreset"))
            assertEquals(4, advanced.getJSONArray("periods").length())

            val parsed = requireNotNull(BackupManager.parse(json))
            assertTrue(BackupSection.BLOCK_RULES in parsed.present)
            assertTrue(
                CallBlockNotificationSettings.replace(
                    context,
                    BlockNotificationAdvancedConfig(scheduleEnabled = false),
                )
            )
            val report = runBlocking {
                BackupManager.restore(
                    context,
                    parsed,
                    setOf(BackupSection.BLOCK_RULES),
                    MergeMode.UPDATE,
                )
            }

            assertEquals(backedUp, CallBlockNotificationSettings.read(context))
            assertEquals(1, report.sections[BackupSection.BLOCK_RULES]?.updated)
        } finally {
            CallBlockNotificationSettings.replace(context, original)
        }
    }

    @Test
    fun customSoundUriIsExportedAsSafePackagedFallback() {
        val original = CallBlockNotificationSettings.read(context)
        try {
            val custom = original.copy(
                defaultAlert = original.defaultAlert.copy(
                    sound = BlockNotificationSound.custom(
                        Uri.parse("content://example.provider/audio/1"),
                        "private-tone.mp3",
                    )
                )
            )
            assertTrue(CallBlockNotificationSettings.replace(context, custom))

            val json = runBlocking {
                BackupManager.buildJson(context, setOf(BackupSection.BLOCK_RULES))
            }
            val storedDefault = JSONObject(json)
                .getJSONObject("sections")
                .getJSONObject("callBlockRules")
                .getJSONObject("advancedNotification")
                .getJSONObject("default")

            assertEquals(BlockNotificationSoundPreset.PULSE.storageKey, storedDefault.getString("soundPreset"))
            assertFalse(storedDefault.toString().contains("content://"))
            assertFalse(json.contains("private-tone.mp3"))
        } finally {
            CallBlockNotificationSettings.replace(context, original)
        }
    }

    @Test
    fun malformedAdvancedNotificationCannotOverwriteBlockSettings() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """{"_format":"mcas-backup","version":6,"sections":{"callBlockRules":{
                    "advancedNotification":{"scheduleEnabled":true,"default":{
                      "soundEnabled":true,"vibrationEnabled":true,
                      "soundPreset":"unknown","presentation":"heads_up"
                    },"periods":[]},"rules":[],"numberEntries":[]
                }}}""".trimIndent()
            )
        )

        assertNull(parsed.blockRules)
        assertFalse(BackupSection.BLOCK_RULES in parsed.present)
    }

    private fun alert(
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        preset: BlockNotificationSoundPreset,
        presentation: BlockNotificationPresentation,
    ) = BlockNotificationAlert(
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        sound = BlockNotificationSound.preset(preset),
        presentation = presentation,
    )
}
