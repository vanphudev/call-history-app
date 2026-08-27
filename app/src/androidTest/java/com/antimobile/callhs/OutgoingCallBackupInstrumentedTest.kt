package com.antimobile.callhs

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antimobile.callhs.data.backup.BackupManager
import com.antimobile.callhs.data.backup.BackupSection
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.outgoing.OutgoingCallConfig
import com.antimobile.callhs.data.outgoing.OutgoingCallPresentation
import com.antimobile.callhs.data.outgoing.OutgoingCallSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OutgoingCallBackupInstrumentedTest {

    @Test
    fun exportParseAndRestorePreservesEveryOutgoingSetting() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val original = OutgoingCallSettings.read(context)
        val backedUp = OutgoingCallConfig(
            enabled = true,
            notifyOffNetwork = false,
            notifyBlocklist = true,
            notifyAllowlist = false,
            presentation = OutgoingCallPresentation.OVERLAY,
        )
        val localBeforeRestore = OutgoingCallConfig(
            enabled = false,
            notifyOffNetwork = true,
            notifyBlocklist = false,
            notifyAllowlist = true,
            presentation = OutgoingCallPresentation.HEADS_UP,
        )

        try {
            OutgoingCallSettings.replace(context, backedUp)
            val json = runBlocking {
                BackupManager.buildJson(context, setOf(BackupSection.OUTGOING_CALL))
            }
            val root = JSONObject(json)
            assertEquals(6, root.getInt("version"))
            val stored = root.getJSONObject("sections").getJSONObject("outgoingCallSettings")
            assertTrue(stored.getBoolean("enabled"))
            assertFalse(stored.getBoolean("notifyOffNetwork"))
            assertTrue(stored.getBoolean("notifyBlocklist"))
            assertFalse(stored.getBoolean("notifyAllowlist"))
            assertEquals("overlay", stored.getString("presentation"))

            val parsed = requireNotNull(BackupManager.parse(json))
            assertTrue(BackupSection.OUTGOING_CALL in parsed.present)
            assertEquals(1, parsed.count(BackupSection.OUTGOING_CALL))

            // ADD is non-destructive for directional settings.
            OutgoingCallSettings.replace(context, localBeforeRestore)
            runBlocking {
                BackupManager.restore(
                    context,
                    parsed,
                    setOf(BackupSection.OUTGOING_CALL),
                    MergeMode.ADD,
                )
            }
            assertEquals(localBeforeRestore, OutgoingCallSettings.read(context))

            // UPDATE/REPLACE apply the complete saved snapshot.
            val report = runBlocking {
                BackupManager.restore(
                    context,
                    parsed,
                    setOf(BackupSection.OUTGOING_CALL),
                    MergeMode.UPDATE,
                )
            }
            assertEquals(backedUp, OutgoingCallSettings.read(context))
            assertEquals(1, report.sections[BackupSection.OUTGOING_CALL]?.updated)
        } finally {
            OutgoingCallSettings.replace(context, original)
        }
    }

    @Test
    fun malformedOutgoingFieldsCannotOverwriteSettings() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """{"_format":"callhs-backup","version":5,"sections":{"outgoingCallSettings":{
                    "enabled":"true","notifyOffNetwork":1,"presentation":"unknown"
                }}}""".trimIndent(),
            ),
        )

        assertNull(parsed.outgoingCall)
        assertFalse(BackupSection.OUTGOING_CALL in parsed.present)
    }
}
