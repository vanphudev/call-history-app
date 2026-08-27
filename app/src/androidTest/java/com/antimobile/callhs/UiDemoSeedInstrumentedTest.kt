package com.antimobile.callhs

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.callhs.data.backup.BackupBlockRule
import com.antimobile.callhs.data.backup.BackupBlockedCall
import com.antimobile.callhs.data.backup.BackupNumberEntry
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockMethod
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.data.blocking.GeographicBlockOption
import com.antimobile.callhs.data.blocking.NumberEntryOrigin
import com.antimobile.callhs.data.blocking.SpecialCallCondition
import com.antimobile.callhs.util.PhoneKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bộ dữ liệu trình diễn có chủ đích cho máy test thật.
 *
 * Test bị bỏ qua mặc định để CI/connectedAndroidTest không tự ý thay đổi dữ liệu. Chỉ chạy khi truyền
 * instrumentation argument `seedUi=true`.
 */
@RunWith(AndroidJUnit4::class)
class UiDemoSeedInstrumentedTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()
    private val context
        get() = instrumentation.targetContext

    @Test
    fun seedUiDemoData() = runBlocking {
        assumeTrue(
            "UI demo seed chỉ chạy khi truyền seedUi=true",
            InstrumentationRegistry.getArguments().getString("seedUi") == "true",
        )

        grantDebugWritePermissions()
        val contactNumbers = seedContacts(context)
        seedCallLog(context, contactNumbers)
        seedBlockingData(context, contactNumbers)

        val repository = CallBlockRepository(context)
        assertEquals(75, repository.observeNumberEntries().first().size)
        assertEquals(8, repository.observeRules().first().size)
        assertEquals(177, repository.observeHistory().first().size)
    }

    private fun grantDebugWritePermissions() {
        listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
        ).forEach { permission ->
            runCatching {
                instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
            }
        }
    }

    private fun seedContacts(context: Context): List<String> {
        val resolver = context.contentResolver
        val seededRawContactIds = linkedSetOf<Long>()
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME} LIKE ?",
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, "%UI Test%"),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) seededRawContactIds += cursor.getLong(0)
        }
        seededRawContactIds.forEach { rawId ->
            resolver.delete(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawId), null, null)
        }

        val aliases = listOf(
            "An", "Bình", "Chi", "Dũng", "Em", "Phúc", "Giang", "Hà", "Iris", "Jolie",
            "Khoa", "Linh", "Minh", "Nga", "Oanh", "Phương", "Quân", "Rin", "Sơn", "Trang",
            "Uyên", "Vân", "William", "Xuân", "Yến", "Zoe",
        )
        val numbers = buildList {
            ('A'..'Z').forEachIndexed { letterIndex, letter ->
                repeat(2) { variant ->
                    val ordinal = letterIndex * 2 + variant + 1
                    val number = "09%08d".format(10_000_000 + ordinal)
                    val name = "$letter - UI Test ${aliases[letterIndex]} ${variant + 1}"
                    val rawUri = resolver.insert(
                        ContactsContract.RawContacts.CONTENT_URI,
                        ContentValues().apply {
                            putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                            putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
                        },
                    ) ?: error("Không tạo được raw contact $name")
                    val rawId = ContentUris.parseId(rawUri)

                    resolver.insert(
                        ContactsContract.Data.CONTENT_URI,
                        ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                        },
                    )
                    resolver.insert(
                        ContactsContract.Data.CONTENT_URI,
                        ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                            put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        },
                    )
                    if (ordinal % 4 == 0) {
                        resolver.insert(
                            ContactsContract.Data.CONTENT_URI,
                            ContentValues().apply {
                                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                put(ContactsContract.CommonDataKinds.Phone.NUMBER, "028%08d".format(ordinal))
                                put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                            },
                        )
                    }
                    if (ordinal % 3 == 0) {
                        resolver.insert(
                            ContactsContract.Data.CONTENT_URI,
                            ContentValues().apply {
                                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                put(ContactsContract.CommonDataKinds.Email.ADDRESS, "ui.test.%02d@example.com".format(ordinal))
                                put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                            },
                        )
                    }
                    if (ordinal % 5 == 0) {
                        resolver.insert(
                            ContactsContract.Data.CONTENT_URI,
                            ContentValues().apply {
                                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                                put(ContactsContract.CommonDataKinds.Organization.COMPANY, "CallHS UI Lab")
                                put(ContactsContract.CommonDataKinds.Organization.TITLE, "Tester $ordinal")
                            },
                        )
                    }
                    add(number)
                }
            }
        }
        val seededCount = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME} LIKE ?",
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, "%UI Test%"),
            null,
        )?.use { it.count } ?: 0
        assertEquals(52, seededCount)
        return numbers
    }

    private fun seedCallLog(context: Context, contactNumbers: List<String>) {
        val resolver = context.contentResolver
        resolver.delete(
            CallLog.Calls.CONTENT_URI,
            "${CallLog.Calls.CACHED_NAME} LIKE ?",
            arrayOf("%UI Test%"),
        )
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val callTypes = intArrayOf(
            CallLog.Calls.INCOMING_TYPE,
            CallLog.Calls.OUTGOING_TYPE,
            CallLog.Calls.MISSED_TYPE,
            CallLog.Calls.REJECTED_TYPE,
            CallLog.Calls.BLOCKED_TYPE,
        )
        repeat(260) { index ->
            val dayOffset = index % 66
            val callType = callTypes[index % callTypes.size]
            val date = today.minusDays(dayOffset.toLong())
                .atTime(LocalTime.of((7 + index * 3) % 24, (index * 11) % 60))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            val contactIndex = (index * 7) % contactNumbers.size
            resolver.insert(
                CallLog.Calls.CONTENT_URI,
                ContentValues().apply {
                    put(CallLog.Calls.NUMBER, contactNumbers[contactIndex])
                    put(CallLog.Calls.TYPE, callType)
                    put(CallLog.Calls.DATE, date)
                    put(
                        CallLog.Calls.DURATION,
                        if (callType == CallLog.Calls.MISSED_TYPE || callType == CallLog.Calls.REJECTED_TYPE) 0 else 18 + (index * 17) % 620,
                    )
                    put(CallLog.Calls.NEW, if (index % 4 == 0) 1 else 0)
                    put(CallLog.Calls.IS_READ, if (index % 6 == 0) 0 else 1)
                    put(CallLog.Calls.CACHED_NAME, "UI Test Call ${contactIndex + 1}")
                },
            ) ?: error("Không ghi được call log thứ $index")
        }
        val seededCount = resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID),
            "${CallLog.Calls.CACHED_NAME} LIKE ?",
            arrayOf("%UI Test%"),
            null,
        )?.use { it.count } ?: 0
        assertEquals(260, seededCount)
    }

    private suspend fun seedBlockingData(context: Context, contactNumbers: List<String>) {
        val repository = CallBlockRepository(context)
        val now = System.currentTimeMillis()
        val origins = NumberEntryOrigin.entries
        val numberEntries = buildList {
            repeat(45) { index ->
                val raw = "090%07d".format(1_000_000 + index)
                add(
                    BackupNumberEntry(
                        action = CallBlockAction.BLOCK.storageKey,
                        rawNumber = raw,
                        phoneKey = PhoneKey.of(raw),
                        displayName = "UI Test Chặn %02d".format(index + 1),
                        origin = origins[index % origins.size].storageKey,
                        enabled = index % 9 != 0,
                        createdAt = now - index * 90_000L,
                    ),
                )
            }
            repeat(30) { index ->
                val raw = contactNumbers[index]
                add(
                    BackupNumberEntry(
                        action = CallBlockAction.ALLOW.storageKey,
                        rawNumber = raw,
                        phoneKey = PhoneKey.of(raw),
                        displayName = "UI Test Cho phép %02d".format(index + 1),
                        origin = origins[(index + 1) % origins.size].storageKey,
                        enabled = index % 11 != 0,
                        createdAt = now - (index + 50) * 90_000L,
                    ),
                )
            }
        }
        val rules = listOf(
            demoRule(CallBlockRuleType.PREFIX, "024", 0, now),
            demoRule(CallBlockRuleType.PREFIX, "028", 1, now),
            demoRule(CallBlockRuleType.SUFFIX, "888", 2, now),
            demoRule(CallBlockRuleType.CONTAINS, "1234", 3, now),
            demoRule(CallBlockRuleType.LENGTH, "11", 4, now),
            demoRule(CallBlockRuleType.CARRIER, "Viettel", 5, now),
            demoRule(
                CallBlockRuleType.GEOGRAPHIC,
                GeographicBlockOption.encode(
                    setOf(GeographicBlockOption.CHINA, GeographicBlockOption.VIETNAM_PREFIX_059),
                ),
                6,
                now,
            ),
            demoRule(
                CallBlockRuleType.SPECIAL,
                SpecialCallCondition.encode(setOf(SpecialCallCondition.PRIVATE_NUMBER)),
                7,
                now,
            ),
        )
        repository.restoreBlockingData(numberEntries, rules, MergeMode.REPLACE)

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val activeDayOffsets = listOf(0, 2, 5, 8, 11, 14, 18, 21, 26, 29)
        val historyReasons = listOf(
            CallBlockRuleType.EXACT_NUMBER to "0901000000",
            CallBlockRuleType.PREFIX to "090",
            CallBlockRuleType.SUFFIX to "888",
            CallBlockRuleType.CONTAINS to "1234",
            CallBlockRuleType.LENGTH to "10",
            CallBlockRuleType.CARRIER to "Viettel",
            CallBlockRuleType.GEOGRAPHIC to GeographicBlockOption.VIETNAM_PREFIX_059.storageKey,
        )
        val history = buildList {
            activeDayOffsets.forEachIndexed { dayIndex, offset ->
                val eventCount = if (offset == 0) 24 else 12 + dayIndex
                repeat(eventCount) { eventIndex ->
                    val numberIndex = (dayIndex * 9 + eventIndex * 5) % contactNumbers.size
                    val blockedAt = today.minusDays(offset.toLong())
                        .atTime(LocalTime.of((6 + eventIndex * 2) % 24, (eventIndex * 7) % 60))
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                    val reason = historyReasons[(dayIndex + eventIndex) % historyReasons.size]
                    add(
                        BackupBlockedCall(
                            rawNumber = contactNumbers[numberIndex],
                            phoneKey = PhoneKey.of(contactNumbers[numberIndex]),
                            blockedAt = blockedAt,
                            ruleType = reason.first.storageKey,
                            ruleValue = reason.second,
                            consecutiveUnanswered = 0,
                            ruleScope = CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey,
                        ),
                    )
                }
            }
        }
        repository.restoreHistory(history, MergeMode.REPLACE)

        CallBlockSettings.init(context)
        CallBlockSettings.setEnabled(context, true)
        CallBlockSettings.setBlockMethod(context, CallBlockMethod.BLOCK_AND_REJECT)
    }

    private fun demoRule(
        type: CallBlockRuleType,
        raw: String,
        order: Int,
        now: Long,
    ) = BackupBlockRule(
        type = type.storageKey,
        rawValue = raw,
        matchValue = raw,
        enabled = order % 5 != 0,
        createdAt = now - order * 120_000L,
        action = CallBlockAction.BLOCK.storageKey,
        scope = CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey,
        userOrder = order,
    )
}
