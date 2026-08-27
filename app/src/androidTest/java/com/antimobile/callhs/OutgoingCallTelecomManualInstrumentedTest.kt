package com.antimobile.callhs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.blocking.SaveNumberEntryResult
import com.antimobile.callhs.data.outgoing.OutgoingCallConfig
import com.antimobile.callhs.data.outgoing.OutgoingCallPresentation
import com.antimobile.callhs.data.outgoing.OutgoingCallSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Chỉ chạy từng method từ adb quanh một ACTION_CALL để kiểm tra callback Telecom end-to-end. */
@RunWith(AndroidJUnit4::class)
class OutgoingCallTelecomManualInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun prepareSyntheticBlocklistCall() = runBlocking {
        assertEquals(
            SaveNumberEntryResult.SAVED,
            CallBlockRepository(context).upsertNumberEntry(
                action = CallBlockAction.BLOCK,
                rawNumber = TEST_NUMBER,
                displayName = "Telecom callback test",
            ),
        )
        OutgoingCallSettings.replace(
            context,
            OutgoingCallConfig(
                enabled = true,
                notifyOffNetwork = false,
                notifyBlocklist = true,
                notifyAllowlist = false,
                presentation = OutgoingCallPresentation.HEADS_UP,
            ),
        )
    }

    @Test
    fun cleanupSyntheticBlocklistCall() = runBlocking {
        val repository = CallBlockRepository(context)
        repository.findEnabledExactNumberEntry(TEST_NUMBER)?.let { repository.deleteNumberEntry(it.id) }
        OutgoingCallSettings.replace(
            context,
            OutgoingCallConfig(
                enabled = true,
                notifyOffNetwork = true,
                notifyBlocklist = true,
                notifyAllowlist = true,
                presentation = OutgoingCallPresentation.HEADS_UP,
            ),
        )
    }

    private companion object {
        const val TEST_NUMBER = "0123456789"
    }
}
