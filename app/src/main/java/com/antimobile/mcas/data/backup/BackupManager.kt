package com.antimobile.mcas.data.backup

import android.content.Context
import android.net.Uri
import com.antimobile.mcas.data.blocking.BlockNotificationMode
import com.antimobile.mcas.data.blocking.BlockNotificationAdvancedConfig
import com.antimobile.mcas.data.blocking.BlockNotificationAlert
import com.antimobile.mcas.data.blocking.BlockNotificationPeriod
import com.antimobile.mcas.data.blocking.BlockNotificationPeriodSettings
import com.antimobile.mcas.data.blocking.BlockNotificationPresentation
import com.antimobile.mcas.data.blocking.BlockNotificationSound
import com.antimobile.mcas.data.blocking.BlockNotificationSoundPreset
import com.antimobile.mcas.data.blocking.ALL_WEEKDAYS_MASK
import com.antimobile.mcas.data.blocking.CallBlockMethod
import com.antimobile.mcas.data.blocking.CallBlockNotificationSettings
import com.antimobile.mcas.data.blocking.CallBlockAction
import com.antimobile.mcas.data.blocking.CallBlockScope
import com.antimobile.mcas.data.blocking.CallBlockRuleMatcher
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.ContactRuleCodec
import com.antimobile.mcas.data.blocking.CallHistoryRuleCodec
import com.antimobile.mcas.data.blocking.NumberEntryOrigin
import com.antimobile.mcas.data.blocking.LEGACY_REPEAT_UNANSWERED_REASON_TYPE
import com.antimobile.mcas.util.PhoneKey
import com.antimobile.mcas.data.blocking.CallBlockRepository
import com.antimobile.mcas.data.blocking.CallBlockSettings
import com.antimobile.mcas.data.blocking.CallBlockDailySchedule
import com.antimobile.mcas.data.blocking.CallBlockScheduleAction
import com.antimobile.mcas.data.blocking.CallBlockSchedulePreset
import com.antimobile.mcas.data.blocking.CallBlockTimeWindow
import com.antimobile.mcas.data.blocking.SavedContactGroupPolicy
import com.antimobile.mcas.data.blocking.UnknownNumberPolicy
import com.antimobile.mcas.data.local.CategoryRepository
import com.antimobile.mcas.data.outgoing.OutgoingCallConfig
import com.antimobile.mcas.data.outgoing.OutgoingCallPresentation
import com.antimobile.mcas.data.outgoing.OutgoingCallSettings
import com.antimobile.mcas.i18n.LangPref
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.ui.theme.ThemeSettings
import com.antimobile.mcas.util.FontScaleSettings
import com.antimobile.mcas.util.MessageTemplate
import com.antimobile.mcas.util.MessageTemplateStore
import com.antimobile.mcas.util.MyNumberStore
import com.antimobile.mcas.util.QrScanEntry
import com.antimobile.mcas.util.QrScanHistoryStore
import com.antimobile.mcas.util.SmsSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * CỖ MÁY SAO LƯU & KHÔI PHỤC — đọc/ghi dữ liệu app tự quản dưới dạng file JSON qua Storage Access Framework
 * (không cần quyền lưu trữ; người dùng tự chọn nơi lưu / file để nhập).
 *
 * Định dạng file (khoá `_format` để nhận diện; mục nào KHÔNG chọn thì vắng khỏi `sections`):
 * ```
 * { "_format":"mcas-backup", "version":6, "appVersion":"…", "createdAt":…,
 *   "sections": {
 *     "templates":[ {"title":…,"content":…} ],
 *     "qrHistory":[ {"raw":…,"time":…} ],
 *     "categories":[ {"name":…,"description":…,"iconKey":…,"colorArgb":…,"builtInKey":…,
 *                     "sortOrder":…,"createdAt":…,"members":[ {"rawNumber":…,"phoneKey":…,"addedAt":…} ]} ],
 *     "callBlockRules":{"enabled":true,"notificationMode":"every","blockMethod":"block_and_reject",
 *                       "repeatUnknownCallerGuardEnabled":false,
 *                       "repeatUnknownCallerGuardThreshold":2,
 *                       "repeatUnknownCallerGuardWindowMinutes":15,
 *                       "advancedNotification":{"scheduleEnabled":false,"default":{…},"periods":[…]},
 *                       "numberEntries":[ {"action":"allow","rawNumber":…,"origin":…} ],
 *                       "rules":[ {"type":…,"rawValue":…,"action":…,"scope":…,"userOrder":…} ]},
 *     "blockedCalls":[ {"rawNumber":…,"phoneKey":…,"blockedAt":…,"ruleType":…,"ruleValue":…} ],
 *     "myNumbers":[ {"slot":0,"number":…} ],
 *     "outgoingCallSettings":{"enabled":true,"notifyOffNetwork":true,"notifyBlocklist":true,
 *                              "notifyAllowlist":true,"presentation":"heads_up"},
 *     "display": {"themePref":…,"langPref":…,"fontScale":…,"smsStrip":…}
 *   } }
 * ```
 * Nhật ký cuộc gọi hệ thống không có trong file; `blockedCalls` chỉ là lịch sử app tự ghi khi xử lý theo quy tắc.
 */
object BackupManager {

    private const val FORMAT = "mcas-backup"
    private const val VERSION = 6
    /** v4 introduced the strict, portable blocker rule contract; unrelated later versions keep it. */
    private const val STRICT_BLOCK_SCHEMA_VERSION = 4

    // --- Xuất ---

    /** Dựng chuỗi JSON cho các [sections] được chọn. Gọi off-main (đọc Room + SharedPreferences). */
    suspend fun buildJson(context: Context, sections: Set<BackupSection>): String {
        val root = JSONObject()
        root.put("_format", FORMAT)
        root.put("version", VERSION)
        root.put("appVersion", appVersionName(context))
        root.put("createdAt", System.currentTimeMillis())

        val secs = JSONObject()
        if (BackupSection.TEMPLATES in sections) {
            secs.put(BackupSection.TEMPLATES.jsonKey, templatesToJson(MessageTemplateStore.load(context)))
        }
        if (BackupSection.QR_HISTORY in sections) {
            secs.put(BackupSection.QR_HISTORY.jsonKey, qrToJson(QrScanHistoryStore.load(context)))
        }
        if (BackupSection.CATEGORIES in sections) {
            secs.put(BackupSection.CATEGORIES.jsonKey, categoriesToJson(CategoryRepository(context).exportForBackup()))
        }
        if (BackupSection.BLOCK_RULES in sections) {
            val repository = CallBlockRepository(context)
            secs.put(
                BackupSection.BLOCK_RULES.jsonKey,
                blockRulesToJson(
                    context = context,
                    rules = repository.exportRulesForBackup(),
                    numberEntries = repository.exportNumberEntriesForBackup(),
                )
            )
        }
        if (BackupSection.BLOCK_HISTORY in sections) {
            secs.put(BackupSection.BLOCK_HISTORY.jsonKey, blockedCallsToJson(CallBlockRepository(context).exportHistoryForBackup()))
        }
        if (BackupSection.MY_NUMBER in sections) {
            secs.put(BackupSection.MY_NUMBER.jsonKey, myNumbersToJson(MyNumberStore.exportAll(context)))
        }
        if (BackupSection.OUTGOING_CALL in sections) {
            secs.put(BackupSection.OUTGOING_CALL.jsonKey, outgoingCallToJson(context))
        }
        if (BackupSection.DISPLAY in sections) {
            secs.put(BackupSection.DISPLAY.jsonKey, displayToJson(context))
        }
        root.put("sections", secs)
        return root.toString(2)
    }

    /** Ghi [text] vào [uri] do người dùng chọn (SAF). @return true nếu ghi xong. */
    fun writeText(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: return false
        true
    }.getOrDefault(false)

    // --- Nhập / phân tích ---

    /** Đọc nội dung văn bản của [uri] (SAF). null nếu lỗi. */
    fun readText(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    /**
     * Phân tích chuỗi JSON thành [ParsedBackup]. Trả null nếu KHÔNG phải file sao lưu hợp lệ (sai `_format`
     * hoặc JSON hỏng). Mục con hỏng riêng lẻ được coi như VẮNG (bỏ qua) để không làm hỏng cả file.
     */
    fun parse(json: String): ParsedBackup? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (root.optString("_format") != FORMAT) return null
        val secs = root.optJSONObject("sections") ?: JSONObject()
        val sourceVersion = root.optInt("version", 1)
        // A newer format may redefine existing fields. Importing it as the current schema could silently
        // enable a rule or discard semantics that this build does not understand.
        if (sourceVersion > VERSION) return null

        // Mục rỗng / hỏng hoàn toàn (mảng rỗng hoặc chỉ toàn phần tử hỏng) → coi như VẮNG (null): tránh bị tính
        // là "có mặt" rồi lọt qua cổng EMPTY_BACKUP và khiến chế độ Ghi-đè XOÁ SẠCH dữ liệu mà không có gì thay vào.
        return ParsedBackup(
            version = sourceVersion,
            appVersion = root.optString("appVersion"),
            createdAt = root.optLong("createdAt"),
            templates = secs.optJSONArray(BackupSection.TEMPLATES.jsonKey)?.let { arr ->
                runCatching { parseTemplates(arr) }.getOrNull()?.takeIf { it.isNotEmpty() }
            },
            qrHistory = secs.optJSONArray(BackupSection.QR_HISTORY.jsonKey)?.let { arr ->
                runCatching { parseQr(arr) }.getOrNull()?.takeIf { it.isNotEmpty() }
            },
            categories = secs.optJSONArray(BackupSection.CATEGORIES.jsonKey)?.let { arr ->
                runCatching { parseCategories(arr) }.getOrNull()?.takeIf { it.isNotEmpty() }
            },
            blockRules = secs.optJSONObject(BackupSection.BLOCK_RULES.jsonKey)?.let { obj ->
                runCatching { parseBlockRules(obj, sourceVersion) }.getOrNull()?.takeIf { it.hasAny }
            },
            blockedCalls = secs.optJSONArray(BackupSection.BLOCK_HISTORY.jsonKey)?.let { arr ->
                runCatching { parseBlockedCalls(arr) }.getOrNull()?.takeIf { it.isNotEmpty() }
            },
            myNumbers = secs.optJSONArray(BackupSection.MY_NUMBER.jsonKey)?.let { arr ->
                runCatching { parseMyNumbers(arr) }.getOrNull()?.takeIf { it.isNotEmpty() }
            },
            outgoingCall = secs.optJSONObject(BackupSection.OUTGOING_CALL.jsonKey)?.let { obj ->
                runCatching { parseOutgoingCall(obj) }.getOrNull()?.takeIf { it.hasAny }
            },
            display = secs.optJSONObject(BackupSection.DISPLAY.jsonKey)?.let { obj ->
                runCatching { parseDisplay(obj) }.getOrNull()?.takeIf { it.hasAny }
            },
        )
    }

    // --- Khôi phục ---

    /** Khôi phục các [sections] được chọn từ [parsed] theo [mode]. Gọi off-main. */
    suspend fun restore(
        context: Context,
        parsed: ParsedBackup,
        sections: Set<BackupSection>,
        mode: MergeMode,
    ): ImportReport {
        val results = LinkedHashMap<BackupSection, SectionResult>()

        if (BackupSection.TEMPLATES in sections && parsed.templates != null) {
            results[BackupSection.TEMPLATES] = MessageTemplateStore.restore(context, parsed.templates, mode)
        }
        if (BackupSection.QR_HISTORY in sections && parsed.qrHistory != null) {
            results[BackupSection.QR_HISTORY] = QrScanHistoryStore.restore(context, parsed.qrHistory, mode)
        }
        if (BackupSection.CATEGORIES in sections && parsed.categories != null) {
            results[BackupSection.CATEGORIES] = CategoryRepository(context).restore(parsed.categories, mode)
        }
        if (BackupSection.BLOCK_RULES in sections && parsed.blockRules != null) {
            // Invalidate the old attempt namespace before restored rules can become visible. A
            // failed restore may reset counters conservatively, but no callback can combine new
            // rules with pre-restore attempts and allow a call early.
            if (mode != MergeMode.ADD) {
                check(CallBlockSettings.resetRepeatUnknownCallerGuardSession(context)) {
                    "Could not invalidate repeated-call attempts before restoring blocking rules"
                }
            }
            val repo = CallBlockRepository(context)
            val rules = repo.restoreBlockingData(
                numberEntries = parsed.blockRules.numberEntries,
                rules = parsed.blockRules.rules,
                mode = mode,
            )
            val settings = restoreBlockSettings(context, parsed.blockRules, mode)
            // ADD/UPDATE can combine a local repeated-caller fallback with an imported
            // BLOCK+ANY+NOT_SAVED group rule. Canonicalize to the higher-priority BLOCK_ALWAYS
            // policy and disable the now-unreachable ledger instead of retaining two toggles.
            if (repo.unknownNumberPolicy() == UnknownNumberPolicy.BLOCK_ALWAYS) {
                check(repo.setUnknownNumberPolicy(UnknownNumberPolicy.BLOCK_ALWAYS)) {
                    "Could not canonicalize outside-Contacts policy after restore"
                }
            }
            results[BackupSection.BLOCK_RULES] = SectionResult(
                added = rules.added,
                updated = rules.updated + settings.updated,
                skipped = rules.skipped + settings.skipped,
                truncated = rules.truncated,
            )
        }
        if (BackupSection.BLOCK_HISTORY in sections && parsed.blockedCalls != null) {
            results[BackupSection.BLOCK_HISTORY] = CallBlockRepository(context).restoreHistory(parsed.blockedCalls, mode)
        }
        if (BackupSection.MY_NUMBER in sections && parsed.myNumbers != null) {
            results[BackupSection.MY_NUMBER] = MyNumberStore.restore(context, parsed.myNumbers, mode)
        }
        if (BackupSection.OUTGOING_CALL in sections && parsed.outgoingCall != null) {
            results[BackupSection.OUTGOING_CALL] = restoreOutgoingCall(context, parsed.outgoingCall, mode)
        }
        if (BackupSection.DISPLAY in sections && parsed.display != null) {
            results[BackupSection.DISPLAY] = restoreDisplay(context, parsed.display, mode)
        }
        return ImportReport(results)
    }

    /**
     * Áp cài đặt hiển thị. Các singleton này giữ snapshot-state của Compose nên PHẢI ghi trên luồng CHÍNH
     * (đổi giao diện/ngôn ngữ/cỡ chữ tức thì cho cả app). [MergeMode.ADD] = giữ nguyên cài đặt hiện tại.
     */
    private suspend fun restoreDisplay(context: Context, d: BackupDisplay, mode: MergeMode): SectionResult {
        if (mode == MergeMode.ADD) return SectionResult(skipped = 1)
        withContext(Dispatchers.Main) {
            d.themePref?.let { pref ->
                runCatching { ThemeSettings.set(context, ThemeSettings.Pref.valueOf(pref)) }
            }
            d.langPref?.let { pref ->
                runCatching { LanguageSettings.set(context, LangPref.valueOf(pref)) }
            }
            d.fontScale?.let { FontScaleSettings.set(context, it) }
            d.smsStrip?.let { SmsSettings.setRemoveDiacritics(context, it) }
        }
        return SectionResult(updated = 1)
    }

    /** ADD giữ nguyên cấu hình hiện tại; UPDATE/REPLACE áp snapshot bằng một lần ghi nguyên tử. */
    private suspend fun restoreOutgoingCall(
        context: Context,
        backup: BackupOutgoingCallConfig,
        mode: MergeMode,
    ): SectionResult {
        if (mode == MergeMode.ADD) return SectionResult(skipped = 1)
        val current = OutgoingCallSettings.read(context)
        val presentation = backup.presentation
            ?.let { raw -> OutgoingCallPresentation.entries.firstOrNull { it.storageKey == raw } }
            ?: current.presentation
        val restored = OutgoingCallConfig(
            enabled = backup.enabled ?: current.enabled,
            notifyOffNetwork = backup.notifyOffNetwork ?: current.notifyOffNetwork,
            notifyBlocklist = backup.notifyBlocklist ?: current.notifyBlocklist,
            notifyAllowlist = backup.notifyAllowlist ?: current.notifyAllowlist,
            presentation = presentation,
        )
        withContext(Dispatchers.Main) {
            OutgoingCallSettings.replace(context, restored)
        }
        return SectionResult(updated = 1)
    }

    /** Cài đặt bộ chặn là singleton app-wide nên áp trên Main để UI phản ánh ngay sau restore. */
    private suspend fun restoreBlockSettings(
        context: Context,
        config: BackupBlockConfig,
        mode: MergeMode,
    ): SectionResult {
        if (!config.hasSettings) return SectionResult()
        if (mode == MergeMode.ADD) return SectionResult(skipped = 1)
        var applied = false
        // v1-v3 stored the saved-contact exception as a global boolean. In v4 it is a real
        // ALLOW+ANY+SAVED_CONTACT group rule, so an explicit legacy false must remove that rule on
        // UPDATE/REPLACE while a missing field must preserve the current policy.
        config.allowSavedContactsEnabled?.let { legacyAllowed ->
            if (
                CallBlockRepository(context).setSavedContactGroupPolicy(
                    if (legacyAllowed) SavedContactGroupPolicy.ALLOW
                    else SavedContactGroupPolicy.FOLLOW_ADVANCED
                )
            ) {
                applied = true
            }
        }
        withContext(Dispatchers.Main) {
            config.enabled?.let {
                CallBlockSettings.setEnabled(context, it)
                applied = true
            }
            config.notificationMode
                ?.let(BlockNotificationMode::fromStorage)
                ?.let {
                    CallBlockSettings.setNotificationMode(context, it)
                    applied = true
                }
            config.blockMethod?.let { raw ->
                val method = CallBlockMethod.fromStorage(raw).let {
                    if (it == CallBlockMethod.ALLOW) CallBlockMethod.BLOCK_AND_REJECT else it
                }
                CallBlockSettings.setBlockMethod(context, method)
                applied = true
            }
            config.repeatUnknownCallerGuardEnabled?.let {
                CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, it)
                applied = true
            }
            config.repeatUnknownCallerGuardThreshold
                ?.takeIf(CallBlockSettings::isValidRepeatUnknownCallerGuardThreshold)
                ?.let {
                    CallBlockSettings.setRepeatUnknownCallerGuardThreshold(context, it)
                    applied = true
                }
            config.repeatUnknownCallerGuardWindowMinutes
                ?.takeIf(CallBlockSettings::isValidRepeatUnknownCallerGuardWindowMinutes)
                ?.let {
                    CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(context, it)
                    applied = true
                }
            config.dailySchedule?.map { window ->
                CallBlockTimeWindow(
                    id = window.id,
                    action = requireNotNull(CallBlockScheduleAction.fromStorage(window.action)),
                    startMinute = window.startMinute,
                    endMinute = window.endMinute,
                    presetKey = window.presetKey,
                    enabled = window.enabled,
                    weekdaysMask = window.weekdaysMask,
                )
            }?.let { schedule ->
                if (CallBlockSettings.replaceDailySchedule(context, schedule)) applied = true
            }
            config.advancedNotification?.let { value ->
                val restored = BlockNotificationAdvancedConfig(
                    defaultAlert = value.defaultAlert.toNotificationAlert(),
                    scheduleEnabled = value.scheduleEnabled,
                    periods = value.periods.map { period ->
                        BlockNotificationPeriodSettings(
                            period = requireNotNull(
                                BlockNotificationPeriod.entries.firstOrNull {
                                    it.storageKey == period.period
                                }
                            ),
                            enabled = period.enabled,
                            alert = period.alert.toNotificationAlert(),
                        )
                    },
                )
                if (CallBlockNotificationSettings.replace(context, restored)) applied = true
            }
        }
        return if (applied) SectionResult(updated = 1) else SectionResult(skipped = 1)
    }

    private fun BackupBlockNotificationAlert.toNotificationAlert(): BlockNotificationAlert =
        BlockNotificationAlert(
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            sound = BlockNotificationSound.preset(
                requireNotNull(BlockNotificationSoundPreset.fromStorage(soundPreset))
            ),
            presentation = requireNotNull(
                BlockNotificationPresentation.entries.firstOrNull { it.storageKey == presentation }
            ),
        )

    // --- JSON: mã hoá ---

    private fun templatesToJson(list: List<MessageTemplate>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("title", it.title).put("content", it.content)) }
        return arr
    }

    private fun qrToJson(list: List<QrScanEntry>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("raw", it.raw).put("time", it.time)) }
        return arr
    }

    private fun categoriesToJson(list: List<BackupCategory>): JSONArray {
        val arr = JSONArray()
        list.forEach { c ->
            val members = JSONArray()
            c.members.forEach { m ->
                members.put(
                    JSONObject().put("rawNumber", m.rawNumber).put("phoneKey", m.phoneKey).put("addedAt", m.addedAt)
                )
            }
            val obj = JSONObject()
                .put("name", c.name)
                .put("description", c.description)
                .put("iconKey", c.iconKey)
                .put("colorArgb", c.colorArgb)
                .put("sortOrder", c.sortOrder)
                .put("createdAt", c.createdAt)
                .put("members", members)
            if (c.builtInKey != null) obj.put("builtInKey", c.builtInKey)
            arr.put(obj)
        }
        return arr
    }

    private fun blockRulesToJson(
        context: Context,
        rules: List<BackupBlockRule>,
        numberEntries: List<BackupNumberEntry>,
    ): JSONObject {
        val repeatGuard = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        val arr = JSONArray()
        // A retired rule may still exist in a pre-migration database opened only for recovery.
        // Never write it into a new backup; v4 imports it only as a narrowly-scoped compatibility
        // exception and cannot recreate it as an active rule.
        rules.filterNot { it.type == LEGACY_REPEAT_UNANSWERED_REASON_TYPE }.forEach { rule ->
            arr.put(
                JSONObject()
                    .put("type", rule.type)
                    .put("rawValue", rule.rawValue)
                    .put("matchValue", rule.matchValue)
                    .put("enabled", rule.enabled)
                    .put("createdAt", rule.createdAt)
                    .put("action", rule.action)
                    .put("scope", rule.scope)
                    .put("userOrder", rule.userOrder)
            )
        }
        val entriesJson = JSONArray()
        numberEntries.forEach { entry ->
            entriesJson.put(
                JSONObject()
                    .put("action", entry.action)
                    .put("rawNumber", entry.rawNumber)
                    .put("phoneKey", entry.phoneKey)
                    .put("displayName", entry.displayName)
                    .put("origin", entry.origin)
                    .put("enabled", entry.enabled)
                    .put("createdAt", entry.createdAt)
            )
        }
        val exportMethod = CallBlockSettings.blockMethod(context).let { method ->
            if (method == CallBlockMethod.ALLOW) CallBlockMethod.BLOCK_AND_REJECT else method
        }
        val dailySchedule = JSONArray().apply {
            CallBlockSettings.dailySchedule(context).forEach { window ->
                put(
                    JSONObject()
                        .put("id", window.id)
                        .put("action", window.action.storageKey)
                        .put("startMinute", window.startMinute)
                        .put("endMinute", window.endMinute)
                        .put("enabled", window.enabled)
                        .put("weekdaysMask", window.weekdaysMask)
                        .apply { window.presetKey?.let { put("presetKey", it) } }
                )
            }
        }
        return JSONObject()
            // Temporary pause metadata is intentionally transient. Export the permanent preference,
            // never the effective false value observed while a 10/30/60-minute pause is active.
            .put(
                "enabled",
                CallBlockSettings.isBaseEnabled(context) &&
                    CallBlockSettings.blockMethod(context) != CallBlockMethod.ALLOW
            )
            .put("notificationMode", CallBlockSettings.notificationMode(context).storageKey)
            .put("blockMethod", exportMethod.storageKey)
            .put("repeatUnknownCallerGuardEnabled", repeatGuard.enabled)
            .put("repeatUnknownCallerGuardThreshold", repeatGuard.threshold)
            .put("repeatUnknownCallerGuardWindowMinutes", repeatGuard.windowMinutes)
            .put("dailySchedule", dailySchedule)
            .put(
                "advancedNotification",
                blockNotificationToJson(CallBlockNotificationSettings.read(context)),
            )
            .put("numberEntries", entriesJson)
            .put("rules", arr)
    }

    private fun blockNotificationToJson(config: BlockNotificationAdvancedConfig): JSONObject =
        JSONObject()
            .put("scheduleEnabled", config.scheduleEnabled)
            .put("default", blockNotificationAlertToJson(config.defaultAlert))
            .put(
                "periods",
                JSONArray().apply {
                    config.periods.forEach { value ->
                        put(
                            JSONObject()
                                .put("period", value.period.storageKey)
                                .put("enabled", value.enabled)
                                .put("alert", blockNotificationAlertToJson(value.alert))
                        )
                    }
                },
            )

    private fun blockNotificationAlertToJson(alert: BlockNotificationAlert): JSONObject =
        JSONObject()
            .put("soundEnabled", alert.soundEnabled)
            .put("vibrationEnabled", alert.vibrationEnabled)
            // A SAF content URI and its persisted grant belong to one Android installation.
            // Back up a safe packaged sound so a transfer never restores an unreadable URI.
            .put("soundPreset", (alert.sound.preset ?: BlockNotificationSoundPreset.PULSE).storageKey)
            .put("presentation", alert.presentation.storageKey)

    private fun blockedCallsToJson(list: List<BackupBlockedCall>): JSONArray {
        val arr = JSONArray()
        list.forEach { call ->
            arr.put(
                JSONObject()
                    .put("rawNumber", call.rawNumber)
                    .put("phoneKey", call.phoneKey)
                    .put("blockedAt", call.blockedAt)
                    .put("ruleType", call.ruleType)
                    .put("ruleValue", call.ruleValue)
                    .put("ruleScope", call.ruleScope)
                    .put("consecutiveUnanswered", call.consecutiveUnanswered)
            )
        }
        return arr
    }

    private fun myNumbersToJson(list: List<MyNumberEntry>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("slot", it.slot).put("number", it.number)) }
        return arr
    }

    private fun displayToJson(context: Context): JSONObject = JSONObject()
        .put("themePref", ThemeSettings.pref.name)
        .put("langPref", LanguageSettings.pref.name)
        .put("fontScale", FontScaleSettings.scale.toDouble())
        .put("smsStrip", SmsSettings.isRemoveDiacritics(context))

    private fun outgoingCallToJson(context: Context): JSONObject {
        val config = OutgoingCallSettings.read(context)
        return JSONObject()
            .put("enabled", config.enabled)
            .put("notifyOffNetwork", config.notifyOffNetwork)
            .put("notifyBlocklist", config.notifyBlocklist)
            .put("notifyAllowlist", config.notifyAllowlist)
            .put("presentation", config.presentation.storageKey)
    }

    // --- JSON: giải mã ---

    private fun parseTemplates(arr: JSONArray): List<MessageTemplate> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val title = o.optString("title")
            val content = o.optString("content")
            if (title.isBlank() && content.isBlank()) null
            else MessageTemplate(id = i + 1L, title = title, content = content)
        }

    private fun parseQr(arr: JSONArray): List<QrScanEntry> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val raw = o.optString("raw")
            if (raw.isBlank()) null else QrScanEntry(raw = raw, time = o.optLong("time"))
        }

    private fun parseCategories(arr: JSONArray): List<BackupCategory> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name")
            val builtInKey = if (o.has("builtInKey") && !o.isNull("builtInKey")) o.optString("builtInKey") else null
            if (name.isBlank() && builtInKey == null) return@mapNotNull null
            val membersArr = o.optJSONArray("members") ?: JSONArray()
            val members = (0 until membersArr.length()).mapNotNull { j ->
                val mo = membersArr.optJSONObject(j) ?: return@mapNotNull null
                val rawNumber = mo.optString("rawNumber")
                if (rawNumber.isBlank()) null
                else BackupMember(rawNumber = rawNumber, phoneKey = mo.optString("phoneKey"), addedAt = mo.optLong("addedAt"))
            }
            BackupCategory(
                name = name,
                description = o.optString("description"),
                iconKey = o.optString("iconKey"),
                colorArgb = o.optLong("colorArgb"),
                builtInKey = builtInKey,
                sortOrder = o.optInt("sortOrder"),
                createdAt = o.optLong("createdAt"),
                members = members,
            )
        }

    private fun parseBlockRules(obj: JSONObject, sourceVersion: Int): BackupBlockConfig {
        val rulesArr = obj.optJSONArray("rules") ?: JSONArray()
        val parsedRules = (0 until rulesArr.length()).mapNotNull { i ->
            val rule = rulesArr.optJSONObject(i) ?: return@mapNotNull null
            val type = rule.optString("type")
            val rawValue = rule.optString("rawValue")
            if (type.isBlank() || rawValue.isBlank()) return@mapNotNull null
            val enabled = rule.optBooleanOrNull("enabled")
                ?: if (sourceVersion < STRICT_BLOCK_SCHEMA_VERSION && !rule.has("enabled")) true else return@mapNotNull null
            BackupBlockRule(
                type = type,
                rawValue = rawValue,
                matchValue = rule.optString("matchValue"),
                enabled = enabled,
                createdAt = rule.optLong("createdAt"),
                action = rule.optStringOrNull("action")
                    ?: if (sourceVersion < STRICT_BLOCK_SCHEMA_VERSION) CallBlockAction.BLOCK.storageKey else return@mapNotNull null,
                scope = rule.optStringOrNull("scope")
                    ?: if (sourceVersion < STRICT_BLOCK_SCHEMA_VERSION) CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey else return@mapNotNull null,
                userOrder = rule.optIntOrNull("userOrder")?.coerceAtLeast(0) ?: i,
            )
        }
        val entriesArr = obj.optJSONArray("numberEntries") ?: JSONArray()
        val parsedEntries = (0 until entriesArr.length()).mapNotNull { i ->
            val entry = entriesArr.optJSONObject(i) ?: return@mapNotNull null
            val action = CallBlockAction.fromStorage(entry.optStringOrNull("action")) ?: return@mapNotNull null
            val rawNumber = entry.optString("rawNumber").trim()
            val phoneKey = PhoneKey.of(rawNumber)
            if (rawNumber.isBlank() || phoneKey.length < 3) return@mapNotNull null
            val enabled = entry.optBooleanOrNull("enabled")
                ?: if (sourceVersion < STRICT_BLOCK_SCHEMA_VERSION && !entry.has("enabled")) true else return@mapNotNull null
            BackupNumberEntry(
                action = action.storageKey,
                rawNumber = rawNumber,
                phoneKey = phoneKey,
                displayName = entry.optString("displayName").trim(),
                origin = NumberEntryOrigin.fromStorage(entry.optStringOrNull("origin"))
                    ?.storageKey ?: NumberEntryOrigin.MANUAL.storageKey,
                enabled = enabled,
                createdAt = entry.optLong("createdAt"),
            )
        }
        // v4 is authoritative app-owned data. Reject the whole section if any item is malformed so
        // REPLACE can never erase valid local data after partially accepting a damaged payload.
        if (sourceVersion >= STRICT_BLOCK_SCHEMA_VERSION) {
            require(
                parsedRules.size == rulesArr.length() &&
                    parsedRules.all {
                        isValidV4Rule(it) ||
                            isRetiredV4RuleForSkip(it) ||
                            isRetiredSpecialV4RuleForAdaptation(it)
                    }
            )
            require(parsedEntries.size == entriesArr.length())
        }
        val (legacyEntries, rules) = adaptLegacyRules(
            rules = parsedRules,
            savedContactsWereAllowed = obj.optBooleanOrNull("allowSavedContactsEnabled") == true,
            sourceVersion = sourceVersion,
        )
        val guardEnabled = obj.optBooleanOrNull("repeatUnknownCallerGuardEnabled")
        if (sourceVersion >= STRICT_BLOCK_SCHEMA_VERSION) {
            val activeGroupRules = rules.filter { rule ->
                rule.enabled && rule.type == CallBlockRuleType.ANY.storageKey &&
                    rule.scope in setOf(
                        CallBlockScope.SAVED_CONTACT.storageKey,
                        CallBlockScope.NOT_SAVED.storageKey,
                    )
            }
            require(activeGroupRules.groupBy(BackupBlockRule::scope).none { it.value.size > 1 })
            val blocksAllUnknown = activeGroupRules.any { rule ->
                rule.scope == CallBlockScope.NOT_SAVED.storageKey &&
                    rule.action == CallBlockAction.BLOCK.storageKey
            }
            require(!(blocksAllUnknown && guardEnabled == true))
        }
        val numberEntries = canonicalNumberEntries(parsedEntries + legacyEntries)
        val rawMethod = obj.optStringOrNull("blockMethod")?.takeIf { raw ->
            CallBlockMethod.entries.any { it.storageKey == raw }
        }
        val legacyAllow = rawMethod == CallBlockMethod.ALLOW.storageKey
        val notificationMode = obj.optStringOrNull("notificationMode")
            ?.takeIf { it in setOf("off", "every", "every_5", "every_10") }
            ?.let { BlockNotificationMode.fromStorage(it).storageKey }
        val scheduleArray = obj.optJSONArray("dailySchedule")
        if (obj.has("dailySchedule") && scheduleArray == null) require(false)
        val parsedSchedule = scheduleArray?.let { array ->
            val windows = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val action = CallBlockScheduleAction.fromStorage(item.optStringOrNull("action"))
                    ?: return@mapNotNull null
                val presetKey = item.optStringOrNull("presetKey")
                if (presetKey != null && CallBlockSchedulePreset.fromStorage(presetKey) == null) {
                    return@mapNotNull null
                }
                BackupBlockScheduleWindow(
                    id = item.optString("id"),
                    action = action.storageKey,
                    startMinute = item.optIntOrNull("startMinute") ?: return@mapNotNull null,
                    endMinute = item.optIntOrNull("endMinute") ?: return@mapNotNull null,
                    presetKey = presetKey,
                    enabled = item.optBooleanOrNull("enabled")
                        ?: if (!item.has("enabled")) true else return@mapNotNull null,
                    weekdaysMask = item.optIntOrNull("weekdaysMask")
                        ?: if (!item.has("weekdaysMask")) ALL_WEEKDAYS_MASK else return@mapNotNull null,
                )
            }
            require(windows.size == array.length())
            require(
                CallBlockDailySchedule.validateAll(
                    windows.map { window ->
                        CallBlockTimeWindow(
                            id = window.id,
                            action = requireNotNull(CallBlockScheduleAction.fromStorage(window.action)),
                            startMinute = window.startMinute,
                            endMinute = window.endMinute,
                            presetKey = window.presetKey,
                            enabled = window.enabled,
                            weekdaysMask = window.weekdaysMask,
                        )
                    }
                )
            )
            windows
        }
        val advancedNotificationObject = obj.optJSONObject("advancedNotification")
        if (obj.has("advancedNotification") && advancedNotificationObject == null) require(false)
        val advancedNotification = advancedNotificationObject?.let(::parseBlockNotification)
        return BackupBlockConfig(
            enabled = if (legacyAllow) false else obj.optBooleanOrNull("enabled"),
            notificationMode = notificationMode,
            blockMethod = if (legacyAllow) {
                CallBlockMethod.BLOCK_AND_REJECT.storageKey
            } else rawMethod,
            rules = rules,
            numberEntries = numberEntries,
            // v4 scope replaces this global bypass. Keep an explicit v1-v3 value only long enough
            // for restore to add/remove the equivalent group policy; v4 never exports this field.
            allowSavedContactsEnabled = if (sourceVersion < STRICT_BLOCK_SCHEMA_VERSION) {
                obj.optBooleanOrNull("allowSavedContactsEnabled")
            } else {
                null
            },
            repeatUnknownCallerGuardEnabled = guardEnabled,
            repeatUnknownCallerGuardThreshold = obj
                .optIntOrNull("repeatUnknownCallerGuardThreshold")
                ?.takeIf(CallBlockSettings::isValidRepeatUnknownCallerGuardThreshold),
            repeatUnknownCallerGuardWindowMinutes = obj
                .optIntOrNull("repeatUnknownCallerGuardWindowMinutes")
                ?.takeIf(CallBlockSettings::isValidRepeatUnknownCallerGuardWindowMinutes),
            dailySchedule = parsedSchedule,
            advancedNotification = advancedNotification,
        )
    }

    private fun parseBlockNotification(obj: JSONObject): BackupBlockNotificationConfig {
        val defaultAlert = obj.optJSONObject("default")
            ?.let(::parseBlockNotificationAlert)
            ?: error("Missing advanced notification default alert")
        val periodsArray = obj.optJSONArray("periods")
            ?: error("Missing advanced notification periods")
        val periods = (0 until periodsArray.length()).map { index ->
            val value = periodsArray.optJSONObject(index)
                ?: error("Invalid advanced notification period")
            val period = value.optStringOrNull("period")
                ?.let { raw -> BlockNotificationPeriod.entries.firstOrNull { it.storageKey == raw } }
                ?: error("Unknown advanced notification period")
            BackupBlockNotificationPeriod(
                period = period.storageKey,
                enabled = value.optBooleanOrNull("enabled")
                    ?: error("Missing advanced notification period state"),
                alert = value.optJSONObject("alert")
                    ?.let(::parseBlockNotificationAlert)
                    ?: error("Missing advanced notification period alert"),
            )
        }
        require(periods.map(BackupBlockNotificationPeriod::period).toSet().size == periods.size)
        require(periods.map(BackupBlockNotificationPeriod::period).toSet() ==
            BlockNotificationPeriod.entries.map(BlockNotificationPeriod::storageKey).toSet())
        return BackupBlockNotificationConfig(
            scheduleEnabled = obj.optBooleanOrNull("scheduleEnabled")
                ?: error("Missing advanced notification schedule state"),
            defaultAlert = defaultAlert,
            periods = periods,
        )
    }

    private fun parseBlockNotificationAlert(obj: JSONObject): BackupBlockNotificationAlert {
        val soundPreset = obj.optStringOrNull("soundPreset")
            ?.let { raw -> BlockNotificationSoundPreset.entries.firstOrNull { it.storageKey == raw } }
            ?: error("Unknown advanced notification sound")
        val presentation = obj.optStringOrNull("presentation")
            ?.let { raw -> BlockNotificationPresentation.entries.firstOrNull { it.storageKey == raw } }
            ?: error("Unknown advanced notification presentation")
        return BackupBlockNotificationAlert(
            soundEnabled = obj.optBooleanOrNull("soundEnabled")
                ?: error("Missing advanced notification sound state"),
            vibrationEnabled = obj.optBooleanOrNull("vibrationEnabled")
                ?: error("Missing advanced notification vibration state"),
            soundPreset = soundPreset.storageKey,
            presentation = presentation.storageKey,
        )
    }

    /** v1-v3 CONTACTS/Call Log/exact rule types are source selectors and become exact entries. */
    private fun adaptLegacyRules(
        rules: List<BackupBlockRule>,
        savedContactsWereAllowed: Boolean,
        sourceVersion: Int,
    ): Pair<List<BackupNumberEntry>, List<BackupBlockRule>> {
        if (sourceVersion >= STRICT_BLOCK_SCHEMA_VERSION) {
            return emptyList<BackupNumberEntry>() to adaptRetiredSpecialV4Rules(rules)
        }
        val entries = ArrayList<BackupNumberEntry>()
        val conditional = ArrayList<BackupBlockRule>()
        val broadScope = CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey
        rules.forEachIndexed { index, rule ->
            val type = CallBlockRuleType.fromStorage(rule.type) ?: return@forEachIndexed
            when (type) {
                CallBlockRuleType.EXACT_NUMBER -> entries += legacyNumberEntry(
                    rawNumber = rule.rawValue,
                    displayName = "",
                    origin = NumberEntryOrigin.LEGACY_MIGRATION,
                    rule = rule,
                ) ?: return@forEachIndexed
                CallBlockRuleType.CONTACTS -> ContactRuleCodec.decode(rule.rawValue).forEach { contact ->
                    contact.numbers.forEach { number ->
                        legacyNumberEntry(number, contact.displayName, NumberEntryOrigin.CONTACT_PICKER, rule)
                            ?.let(entries::add)
                    }
                }
                CallBlockRuleType.CALL_HISTORY -> CallHistoryRuleCodec.decode(rule.rawValue).forEach { selected ->
                    legacyNumberEntry(
                        selected.rawNumber,
                        selected.displayName,
                        NumberEntryOrigin.CALL_LOG_PICKER,
                        rule,
                    )?.let(entries::add)
                }
                CallBlockRuleType.SPECIAL -> {
                    com.antimobile.mcas.data.blocking.SpecialCallCondition.decode(rule.rawValue).forEach { condition ->
                        if (condition == com.antimobile.mcas.data.blocking.SpecialCallCondition.UNKNOWN_CONTACT) {
                            conditional += BackupBlockRule(
                                type = CallBlockRuleType.ANY.storageKey,
                                rawValue = "any",
                                matchValue = "any",
                                enabled = rule.enabled,
                                createdAt = rule.createdAt,
                                scope = CallBlockScope.NOT_SAVED.storageKey,
                                userOrder = index + 1,
                            )
                        } else {
                            val replacements = if (
                                condition == com.antimobile.mcas.data.blocking.SpecialCallCondition.VOIP
                            ) {
                                listOf(
                                    com.antimobile.mcas.data.blocking.SpecialCallCondition.SIP_PHONE_NUMBER,
                                    com.antimobile.mcas.data.blocking.SpecialCallCondition.SIP_TEXT_ID,
                                )
                            } else {
                                listOf(condition)
                            }
                            replacements.forEachIndexed { offset, replacement ->
                                val raw = com.antimobile.mcas.data.blocking.SpecialCallCondition.encode(
                                    setOf(replacement)
                                )
                                conditional += rule.copy(
                                    rawValue = raw,
                                    matchValue = raw,
                                    scope = if (
                                        replacement in setOf(
                                            com.antimobile.mcas.data.blocking.SpecialCallCondition.PRIVATE_NUMBER,
                                            com.antimobile.mcas.data.blocking.SpecialCallCondition.SIP_TEXT_ID,
                                        )
                                    ) {
                                        CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey
                                    } else {
                                        rule.scope
                                    },
                                    userOrder = index + 1 + offset,
                                )
                            }
                        }
                    }
                }
                else -> conditional += rule.copy(scope = broadScope, userOrder = index + 1)
            }
        }
        if (savedContactsWereAllowed) {
            conditional += BackupBlockRule(
                type = CallBlockRuleType.ANY.storageKey,
                rawValue = "any",
                matchValue = "any",
                enabled = true,
                createdAt = 0L,
                action = CallBlockAction.ALLOW.storageKey,
                scope = CallBlockScope.SAVED_CONTACT.storageKey,
                userOrder = 0,
            )
        }
        return entries to conditional.filter(::isValidV4Rule)
    }

    /**
     * Shipped v4 files could contain SPECIAL/unknown_contact or contact-scoped SPECIAL rules.
     * Preserve the one supported behavior as the outside-Contacts group policy. Old broad VoIP is
     * split into current SIP-phone/SIP-text rules; phone scope is retained and text scope becomes ALL.
     */
    private fun adaptRetiredSpecialV4Rules(rules: List<BackupBlockRule>): List<BackupBlockRule> {
        val adapted = ArrayList<BackupBlockRule>()
        var hasOutsideContactsGroup = rules.any { rule ->
            rule.enabled &&
            rule.type == CallBlockRuleType.ANY.storageKey &&
                rule.scope == CallBlockScope.NOT_SAVED.storageKey
        }
        rules.forEach { rule ->
            if (isValidV4Rule(rule)) {
                if (
                    !rule.enabled &&
                    rule.type == CallBlockRuleType.ANY.storageKey &&
                    rule.scope in setOf(
                        CallBlockScope.SAVED_CONTACT.storageKey,
                        CallBlockScope.NOT_SAVED.storageKey,
                    )
                ) return@forEach
                adapted += rule
                return@forEach
            }
            if (!isRetiredSpecialV4RuleForAdaptation(rule)) return@forEach

            val conditions = com.antimobile.mcas.data.blocking.SpecialCallCondition.decode(rule.rawValue)
            val activeConditions = buildList {
                conditions.forEach { condition ->
                    when (condition) {
                        com.antimobile.mcas.data.blocking.SpecialCallCondition.VOIP -> {
                            add(com.antimobile.mcas.data.blocking.SpecialCallCondition.SIP_PHONE_NUMBER)
                            add(com.antimobile.mcas.data.blocking.SpecialCallCondition.SIP_TEXT_ID)
                        }
                        in com.antimobile.mcas.data.blocking.SpecialCallCondition.activeEntries -> add(condition)
                        else -> Unit
                    }
                }
            }.distinct()
            activeConditions.forEachIndexed { offset, condition ->
                val raw = com.antimobile.mcas.data.blocking.SpecialCallCondition.encode(setOf(condition))
                val canonicalScope = if (
                    condition in setOf(
                        com.antimobile.mcas.data.blocking.SpecialCallCondition.PRIVATE_NUMBER,
                        com.antimobile.mcas.data.blocking.SpecialCallCondition.SIP_TEXT_ID,
                    )
                ) {
                    CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey
                } else {
                    rule.scope
                }
                adapted += rule.copy(
                    rawValue = raw,
                    matchValue = raw,
                    scope = canonicalScope,
                    userOrder = rule.userOrder + offset,
                )
            }
            if (
                !hasOutsideContactsGroup &&
                com.antimobile.mcas.data.blocking.SpecialCallCondition.UNKNOWN_CONTACT in conditions &&
                rule.enabled &&
                rule.action == CallBlockAction.BLOCK.storageKey &&
                rule.scope != CallBlockScope.SAVED_CONTACT.storageKey
            ) {
                adapted += BackupBlockRule(
                    type = CallBlockRuleType.ANY.storageKey,
                    rawValue = "any",
                    matchValue = "any",
                    enabled = true,
                    createdAt = rule.createdAt,
                    action = CallBlockAction.BLOCK.storageKey,
                    scope = CallBlockScope.NOT_SAVED.storageKey,
                    userOrder = 0,
                )
                hasOutsideContactsGroup = true
            }
        }
        return adapted.filter(::isValidV4Rule)
    }

    private fun legacyNumberEntry(
        rawNumber: String,
        displayName: String,
        origin: NumberEntryOrigin,
        rule: BackupBlockRule,
    ): BackupNumberEntry? {
        val raw = rawNumber.trim()
        val key = PhoneKey.of(raw)
        if (raw.isEmpty() || key.length < 3) return null
        return BackupNumberEntry(
            action = CallBlockAction.BLOCK.storageKey,
            rawNumber = raw,
            phoneKey = key,
            displayName = displayName.trim(),
            origin = origin.storageKey,
            enabled = rule.enabled,
            createdAt = rule.createdAt,
        )
    }

    private fun isValidV4Rule(rule: BackupBlockRule): Boolean {
        val type = CallBlockRuleType.fromStorage(rule.type) ?: return false
        if (type in setOf(CallBlockRuleType.EXACT_NUMBER, CallBlockRuleType.CONTACTS, CallBlockRuleType.CALL_HISTORY)) {
            return false
        }
        val action = CallBlockAction.fromStorage(rule.action) ?: return false
        return type.supportsAction(action) &&
            CallBlockScope.fromStorage(rule.scope)?.let { scope ->
                type.supportsScope(scope, rule.rawValue)
            } == true &&
            CallBlockRuleMatcher.isValid(type, rule.rawValue)
    }

    private fun isRetiredSpecialV4RuleForAdaptation(rule: BackupBlockRule): Boolean {
        if (rule.type != CallBlockRuleType.SPECIAL.storageKey) return false
        val action = CallBlockAction.fromStorage(rule.action) ?: return false
        if (!CallBlockRuleType.SPECIAL.supportsAction(action)) return false
        if (CallBlockScope.fromStorage(rule.scope) == null) return false
        return com.antimobile.mcas.data.blocking.SpecialCallCondition.isLegacyPayload(rule.rawValue)
    }

    /**
     * Backups already shipped as v4 may contain the rule retired by database v4. Accept only its
     * exact canonical shape so the parser can skip it while still rejecting unknown/corrupt rows.
     */
    private fun isRetiredV4RuleForSkip(rule: BackupBlockRule): Boolean =
        rule.type == LEGACY_REPEAT_UNANSWERED_REASON_TYPE &&
            rule.rawValue == "5" &&
            rule.matchValue == "5" &&
            CallBlockAction.fromStorage(rule.action) != null &&
            CallBlockScope.fromStorage(rule.scope) != null

    /** Corrupt/hand-edited overlap is resolved deterministically: ALLOW wins for the same PhoneKey. */
    private fun canonicalNumberEntries(entries: List<BackupNumberEntry>): List<BackupNumberEntry> = entries
        .groupBy { PhoneKey.of(it.rawNumber) }
        .filterKeys { it.isNotBlank() }
        .mapNotNull { (_, duplicates) ->
            duplicates
                .filter { CallBlockAction.fromStorage(it.action) != null }
                .sortedWith(
                    compareBy<BackupNumberEntry> {
                        if (it.action == CallBlockAction.ALLOW.storageKey) 0 else 1
                    }.thenByDescending { it.createdAt }
                )
                .firstOrNull()
        }
        .sortedWith(compareBy<BackupNumberEntry> { it.createdAt }.thenBy { it.phoneKey })

    private fun parseBlockedCalls(arr: JSONArray): List<BackupBlockedCall> =
        (0 until arr.length()).mapNotNull { i ->
            val call = arr.optJSONObject(i) ?: return@mapNotNull null
            val rawNumber = call.optString("rawNumber")
            val ruleType = call.optString("ruleType")
            if (rawNumber.isBlank() || ruleType.isBlank()) return@mapNotNull null
            BackupBlockedCall(
                rawNumber = rawNumber,
                phoneKey = call.optString("phoneKey"),
                blockedAt = call.optLong("blockedAt"),
                ruleType = ruleType,
                ruleValue = call.optString("ruleValue"),
                consecutiveUnanswered = call.optInt("consecutiveUnanswered").coerceAtLeast(0),
                ruleScope = CallBlockScope.fromStorage(call.optStringOrNull("ruleScope"))
                    ?.storageKey ?: CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey,
            )
        }

    private fun parseMyNumbers(arr: JSONArray): List<MyNumberEntry> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val number = o.optString("number")
            if (number.isBlank()) null else MyNumberEntry(slot = o.optInt("slot"), number = number)
        }

    private fun parseDisplay(o: JSONObject): BackupDisplay = BackupDisplay(
        themePref = o.optStringOrNull("themePref"),
        langPref = o.optStringOrNull("langPref"),
        // optDouble trả Double.NaN cho giá trị không phải số → loại bỏ (NaN lọt xuống sẽ làm hỏng cỡ chữ toàn app).
        fontScale = if (o.has("fontScale") && !o.isNull("fontScale")) {
            o.optDouble("fontScale").toFloat().takeIf { it.isFinite() }
        } else null,
        smsStrip = if (o.has("smsStrip") && !o.isNull("smsStrip")) o.optBoolean("smsStrip") else null,
    )

    private fun parseOutgoingCall(o: JSONObject): BackupOutgoingCallConfig {
        val presentation = o.optStringOrNull("presentation")
            ?.takeIf { raw -> OutgoingCallPresentation.entries.any { it.storageKey == raw } }
        return BackupOutgoingCallConfig(
            enabled = o.optBooleanOrNull("enabled"),
            notifyOffNetwork = o.optBooleanOrNull("notifyOffNetwork"),
            notifyBlocklist = o.optBooleanOrNull("notifyBlocklist"),
            notifyAllowlist = o.optBooleanOrNull("notifyAllowlist"),
            presentation = presentation,
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    /** Không ép chuỗi/số thành false: field sai kiểu được xem như vắng để không ghi đè setting hiện tại. */
    private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
        if (has(key) && !isNull(key)) opt(key) as? Boolean else null

    /** Chỉ nhận JSON integer thật; không ép chuỗi, boolean hay số thực thành cấu hình. */
    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Int -> value
            is Long -> value
                .takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                ?.toInt()
            else -> null
        }
    }

    private fun appVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
}
