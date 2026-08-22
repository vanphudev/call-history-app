package com.antimobile.callhs.data.blocking

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.antimobile.callhs.util.Carrier
import com.antimobile.callhs.util.PhoneKey
import java.net.URLDecoder
import java.util.Base64
import java.util.Locale

/**
 * Kiểu quy tắc chặn được app đánh giá hoàn toàn cục bộ trước khi điện thoại đổ chuông.
 *
 * Giá trị [storageKey] là hợp đồng dữ liệu bền vững cho Room/backup; không đổi khi đổi
 * ngôn ngữ hiển thị. Thứ tự ưu tiên được giữ ở [priority]: một số cụ thể luôn thắng một
 * mẫu bao quát hơn để lịch sử nói đúng lý do cuộc gọi bị chặn.
 */
enum class CallBlockRuleType(val storageKey: String, val priority: Int) {
    EXACT_NUMBER("exact", 0),
    CONTACTS("contacts", 0),
    CALL_HISTORY("call_history", 0),
    /** Group rule. Its scope decides whether saved, not-saved, or all visible numbers match. */
    ANY("any", 1),
    PREFIX("prefix", 2),
    SUFFIX("suffix", 3),
    CONTAINS("contains", 4),
    LENGTH("length", 5),
    CARRIER("carrier", 6),
    GEOGRAPHIC("geographic", 6),
    SPECIAL("special", 1),
    BRAND_NAME("brand_name", 1),
    /** Offline risk profile maintained by the app. This type only supports [CallBlockAction.BLOCK]. */
    SPAM_RISK("spam_risk", 7);

    companion object {
        fun fromStorage(key: String?): CallBlockRuleType? =
            entries.firstOrNull { it.storageKey == key }
    }

    fun supportsAction(action: CallBlockAction): Boolean =
        this != SPAM_RISK || action == CallBlockAction.BLOCK

    /** Contact scope is meaningful only when the criterion has a stable telephone identity. */
    fun supportsScope(scope: CallBlockScope, rawValue: String? = null): Boolean = when (this) {
        BRAND_NAME -> scope == CallBlockScope.ALL_VISIBLE_NUMBERS
        SPECIAL ->
            SpecialCallCondition.activeSelection(rawValue.orEmpty()) !in setOf(
                SpecialCallCondition.PRIVATE_NUMBER,
                SpecialCallCondition.SIP_TEXT_ID,
            ) || scope == CallBlockScope.ALL_VISIBLE_NUMBERS
        else -> true
    }
}

/** Stable rule/list decision. Exact ALLOW entries are always evaluated before every BLOCK entry. */
enum class CallBlockAction(val storageKey: String) {
    ALLOW("allow"),
    BLOCK("block");

    companion object {
        fun fromStorage(key: String?): CallBlockAction? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/**
 * Contact membership is a property of the incoming caller, never a picker/source rule type.
 * A failed or unavailable lookup matches only [ALL_VISIBLE_NUMBERS], never [NOT_SAVED].
 */
enum class CallBlockScope(val storageKey: String) {
    SAVED_CONTACT("saved_contact"),
    NOT_SAVED("not_saved"),
    ALL_VISIBLE_NUMBERS("all_visible");

    companion object {
        fun fromStorage(key: String?): CallBlockScope? =
            entries.firstOrNull { it.storageKey == key }
    }

    fun matches(status: ContactLookupStatus): Boolean = when (this) {
        SAVED_CONTACT -> status == ContactLookupStatus.IN_CONTACTS
        NOT_SAVED -> status == ContactLookupStatus.NOT_IN_CONTACTS
        ALL_VISIBLE_NUMBERS -> true
    }
}

/** Picker provenance is presentation/audit metadata only and never participates in matching. */
enum class NumberEntryOrigin(val storageKey: String) {
    MANUAL("manual"),
    CONTACT_PICKER("contact_picker"),
    CALL_LOG_PICKER("call_log_picker"),
    CATEGORY_PICKER("category_picker"),
    LEGACY_MIGRATION("legacy_migration");

    companion object {
        fun fromStorage(key: String?): NumberEntryOrigin? =
            entries.firstOrNull { it.storageKey == key }
    }
}

/** Stable grouping used by UI to present one GEOGRAPHIC rule in three neutral sections. */
enum class GeographicBlockKind { INTERNATIONAL_PRESET, COUNTRY_CALLING_CODE, VIETNAM_PREFIX }

/**
 * Stable, locale-independent options stored by a GEOGRAPHIC rule. Matching is OR.
 *
 * Country entries describe caller-ID calling-code prefixes only; they are not a fraud verdict.
 * `022` is a Vietnamese fixed-line prefix family and `059`/`099` are mobile prefixes, so they are
 * deliberately modelled as generic [GeographicBlockKind.VIETNAM_PREFIX] values.
 */
enum class GeographicBlockOption(
    val storageKey: String,
    val kind: GeographicBlockKind,
    val callingCode: String? = null,
    val domesticPrefix: String? = null,
) {
    ALL_INTERNATIONAL_EXCEPT_VIETNAM(
        "international_except_vietnam",
        GeographicBlockKind.INTERNATIONAL_PRESET,
    ),
    CHINA("cn", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "86"),
    CAMBODIA("kh", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "855"),
    MYANMAR("mm", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "95"),
    NANP_SHARED("nanp", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "1"),
    GERMANY("de", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "49"),
    LAOS("la", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "856"),
    THAILAND("th", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "66"),
    MALAYSIA("my", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "60"),
    SINGAPORE("sg", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "65"),
    INDONESIA("id", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "62"),
    PHILIPPINES("ph", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "63"),
    INDIA("in", GeographicBlockKind.COUNTRY_CALLING_CODE, callingCode = "91"),
    VIETNAM_PREFIX_024("024", GeographicBlockKind.VIETNAM_PREFIX, domesticPrefix = "024"),
    VIETNAM_PREFIX_022("022", GeographicBlockKind.VIETNAM_PREFIX, domesticPrefix = "022"),
    VIETNAM_PREFIX_028("028", GeographicBlockKind.VIETNAM_PREFIX, domesticPrefix = "028"),
    VIETNAM_PREFIX_059("059", GeographicBlockKind.VIETNAM_PREFIX, domesticPrefix = "059"),
    VIETNAM_PREFIX_099("099", GeographicBlockKind.VIETNAM_PREFIX, domesticPrefix = "099");

    companion object {
        fun decode(raw: String): Set<GeographicBlockOption> {
            if (raw.isBlank()) return emptySet()
            val result = LinkedHashSet<GeographicBlockOption>()
            raw.split(',').map(String::trim).filter(String::isNotEmpty).forEach { key ->
                entries.firstOrNull { it.storageKey == key }?.let(result::add)
            }
            return result
        }

        /**
         * Declaration order is the persisted order. The all-international preset subsumes explicit
         * country entries, but Vietnamese-prefix selections remain independent and are preserved.
         */
        fun encode(values: Set<GeographicBlockOption>): String {
            val hasInternationalPreset = ALL_INTERNATIONAL_EXCEPT_VIETNAM in values
            return entries.asSequence()
                .filter(values::contains)
                .filterNot {
                    hasInternationalPreset && it.kind == GeographicBlockKind.COUNTRY_CALLING_CODE
                }
                .joinToString(",") { it.storageKey }
        }

        internal fun isValidPayload(raw: String): Boolean {
            val tokens = raw.split(',').map(String::trim).filter(String::isNotEmpty)
            return tokens.isNotEmpty() && tokens.all { token -> entries.any { it.storageKey == token } }
        }
    }
}

/** Stable multi-select conditions used by a SPECIAL rule. Matching is OR. */
enum class SpecialCallCondition(val storageKey: String) {
    PRIVATE_NUMBER("private"),
    /** History/backup compatibility only. Active outside-Contacts behavior is an ANY/NOT_SAVED group rule. */
    UNKNOWN_CONTACT("unknown_contact"),
    /** Compatibility token for the old broad VoIP option; new rules classify SIP identity shape. */
    VOIP("voip"),
    SIP_PHONE_NUMBER("sip_phone"),
    SIP_TEXT_ID("sip_text");

    companion object {
        /** Conditions that can be created as an active SPECIAL rule. */
        val activeEntries: List<SpecialCallCondition> = listOf(
            PRIVATE_NUMBER,
            SIP_PHONE_NUMBER,
            SIP_TEXT_ID,
        )

        fun decode(raw: String): Set<SpecialCallCondition> {
            if (raw.isBlank()) return emptySet()
            val result = LinkedHashSet<SpecialCallCondition>()
            raw.split(',').map(String::trim).filter(String::isNotEmpty).forEach { key ->
                entries.firstOrNull { it.storageKey == key }?.let(result::add)
            }
            return result
        }

        /** Active SPECIAL rules are deliberately single-select. */
        fun activeSelection(raw: String): SpecialCallCondition? =
            decode(raw).singleOrNull()?.takeIf(activeEntries::contains)

        /** Canonical order is declaration order, independent from locale and UI ordering. */
        fun encode(values: Set<SpecialCallCondition>): String =
            entries.filter(values::contains).joinToString(",") { it.storageKey }

        fun canonical(raw: String): String = encode(decode(raw))

        internal fun isValidPayload(raw: String): Boolean {
            val tokens = raw.split(',').map(String::trim).filter(String::isNotEmpty)
            return tokens.size == 1 && activeEntries.any { it.storageKey == tokens.single() }
        }

        /** Accepts the retired unknown_contact token only while upgrading shipped data. */
        internal fun isLegacyPayload(raw: String): Boolean {
            val tokens = raw.split(',').map(String::trim).filter(String::isNotEmpty)
            return tokens.isNotEmpty() && tokens.all { token -> entries.any { it.storageKey == token } }
        }

    }
}

enum class SipCallerIdKind { PHONE_NUMBER, TEXT_ID, UNKNOWN }

data class SipCallerIdentity(
    val kind: SipCallerIdKind,
    /** Decoded SIP user part before `@`; never contains a password. */
    val user: String = "",
    /** Populated only when [kind] is [SipCallerIdKind.PHONE_NUMBER]. */
    val phoneNumber: String? = null,
) {
    companion object {
        val UNKNOWN = SipCallerIdentity(SipCallerIdKind.UNKNOWN)
    }
}

/** Pure parser for the small portion of RFC 3261 needed by screening decisions. */
object SipCallerIdentityParser {
    private const val MAX_URI_LENGTH = 512
    private const val MAX_USER_LENGTH = 128

    fun parse(scheme: String?, schemeSpecificPart: String?): SipCallerIdentity {
        if (scheme?.lowercase(Locale.ROOT) !in setOf("sip", "sips")) return SipCallerIdentity.UNKNOWN
        val address = schemeSpecificPart.orEmpty().trim().removePrefix("//")
        if (address.isEmpty() || address.length > MAX_URI_LENGTH) return SipCallerIdentity.UNKNOWN
        val at = address.lastIndexOf('@')
        if (at <= 0 || address.substring(0, at).contains('@')) return SipCallerIdentity.UNKNOWN
        val hostAndParameters = address.substring(at + 1).substringBefore('?')
        val host = hostAndParameters.substringBefore(';')
        if (host.isBlank() || host.any { it.isWhitespace() || it.isISOControl() }) {
            return SipCallerIdentity.UNKNOWN
        }

        // RFC 3261 userinfo can be user[:password]. Subscriber parameters such as postd can be
        // attached to a phone user; neither password nor parameters belong in Contacts lookup.
        val encodedUser = address.substring(0, at).substringBefore(':').substringBefore(';')
        val user = percentDecode(encodedUser).trim()
        if (
            user.isEmpty() ||
            user.length > MAX_USER_LENGTH ||
            user.any { it.isWhitespace() || it.isISOControl() }
        ) {
            return SipCallerIdentity.UNKNOWN
        }

        val userParameters = hostAndParameters.split(';').drop(1)
            .filter { parameter -> parameter.substringBefore('=').equals("user", ignoreCase = true) }
        if (userParameters.size > 1) return SipCallerIdentity.UNKNOWN
        val declaredUserType = userParameters.singleOrNull()?.split('=', limit = 2)
            ?.getOrNull(1)
            ?.takeIf(String::isNotEmpty)
            ?.lowercase(Locale.ROOT)
            ?: if (userParameters.isNotEmpty()) return SipCallerIdentity.UNKNOWN else null
        val looksLikePhone = CallHistoryRuleCodec.isSelectableNumber(user)
        return when {
            declaredUserType == "phone" && looksLikePhone ->
                SipCallerIdentity(SipCallerIdKind.PHONE_NUMBER, user = user, phoneNumber = user)
            declaredUserType == "phone" -> SipCallerIdentity.UNKNOWN
            declaredUserType != null -> SipCallerIdentity(SipCallerIdKind.TEXT_ID, user = user)
            looksLikePhone -> SipCallerIdentity(SipCallerIdKind.PHONE_NUMBER, user = user, phoneNumber = user)
            else -> SipCallerIdentity(SipCallerIdKind.TEXT_ID, user = user)
        }
    }

    private fun percentDecode(value: String): String = runCatching {
        // URLDecoder otherwise converts a literal '+' into a space; in SIP phone users '+' is data.
        URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
    }.getOrDefault(value)
}

/** Stable history/count key for either a telephone number or a supported SIP text identity. */
object BlockedCallerIdentity {
    fun key(rawIdentity: String): String? {
        val raw = rawIdentity.trim()
        if (CallHistoryRuleCodec.isSelectableNumber(raw)) return PhoneKey.of(raw).takeIf(String::isNotEmpty)
        val separator = raw.indexOf(':')
        if (separator <= 0) return null
        val sipIdentity = SipCallerIdentityParser.parse(
            scheme = raw.substring(0, separator),
            schemeSpecificPart = raw.substring(separator + 1),
        )
        if (sipIdentity.kind != SipCallerIdKind.TEXT_ID) return null
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.toByteArray(Charsets.UTF_8))
        return "uri:$encoded"
    }
}

/** Versioned payload for an independent, exact and case-sensitive Brandname rule. */
object BrandNameRuleCodec {
    const val MAX_NAMES = 5
    const val MAX_NAME_LENGTH = 64
    private const val VERSION = "v1"

    fun decode(raw: String): List<String> {
        val parts = raw.split('|')
        if (parts.firstOrNull() != VERSION) return emptyList()
        return parts.drop(1)
            .mapNotNull(::decodePart)
            .map(String::trim)
            .filter(::isAllowedName)
            .distinct()
    }

    fun encode(names: List<String>): String {
        // A Brandname rule is a set. Stable case-sensitive sorting prevents duplicates whose only
        // difference is the order in which the user selected the same names.
        val normalized = names.map(String::trim).filter(::isAllowedName).distinct().sorted()
        return buildString {
            append(VERSION)
            normalized.forEach { name ->
                append('|')
                append(encodePart(name))
            }
        }
    }

    fun canonical(raw: String): String = encode(decode(raw))

    fun isAllowedName(value: String): Boolean {
        val name = value.trim()
        return name.isNotEmpty() && name.length <= MAX_NAME_LENGTH && name.none(Char::isISOControl)
    }

    internal fun isValidPayload(raw: String): Boolean {
        val names = decode(raw)
        return names.isNotEmpty() && names.size <= MAX_NAMES && raw == encode(names)
    }

    private fun encodePart(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String? = runCatching {
        Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
    }.getOrNull()
}

/** Stable reason recorded when the app-maintained spam-risk profile matches a call. */
enum class SpamRiskReasonKind(val storageKey: String) {
    PREFIX("prefix"),
    UNKNOWN_MOBILE_PREFIX("unknown_mobile_prefix"),
    VERIFICATION_FAILED("verification_failed");

    companion object {
        fun fromStorage(key: String?): SpamRiskReasonKind? =
            entries.firstOrNull { it.storageKey == key }
    }
}

data class SpamRiskReason(
    val kind: SpamRiskReasonKind,
    /** Three-digit domestic prefix for PREFIX/UNKNOWN_MOBILE_PREFIX; empty for verification. */
    val prefix: String = "",
)

/** Versioned history/notification payload. Rule raw/match storage remains `app_default`. */
object SpamRiskReasonCodec {
    private const val VERSION = "v1"

    fun encode(reason: SpamRiskReason): String = when (reason.kind) {
        SpamRiskReasonKind.PREFIX,
        SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX,
        -> {
            require(reason.prefix.length == 3 && reason.prefix.all(Char::isDigit))
            "$VERSION|${reason.kind.storageKey}|${reason.prefix}"
        }
        SpamRiskReasonKind.VERIFICATION_FAILED -> {
            require(reason.prefix.isEmpty())
            "$VERSION|${reason.kind.storageKey}"
        }
    }

    fun decode(raw: String): SpamRiskReason? {
        val parts = raw.split('|')
        if (parts.firstOrNull() != VERSION) return null
        val kind = SpamRiskReasonKind.fromStorage(parts.getOrNull(1)) ?: return null
        return when (kind) {
            SpamRiskReasonKind.PREFIX,
            SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX,
            -> {
                val prefix = parts.getOrNull(2)
                    ?.takeIf { parts.size == 3 && it.length == 3 && it.all(Char::isDigit) }
                    ?: return null
                when (kind) {
                    SpamRiskReasonKind.PREFIX -> prefix.takeIf(KNOWN_PROFILE_PREFIXES::contains)
                    SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX -> prefix.takeIf { candidate ->
                        candidate.startsWith('0') &&
                            candidate.getOrNull(1)?.let(MOBILE_NAMESPACE_DIGITS::contains) == true
                    }
                    SpamRiskReasonKind.VERIFICATION_FAILED -> null
                }?.let { SpamRiskReason(kind, it) }
            }
            SpamRiskReasonKind.VERIFICATION_FAILED ->
                SpamRiskReason(kind).takeIf { parts.size == 2 }
        }
    }

    private val KNOWN_PROFILE_PREFIXES = setOf("022", "023", "024", "028", "059", "099")
    private val MOBILE_NAMESPACE_DIGITS = setOf('3', '5', '7', '8', '9')
}

/** Portable snapshot; Android contact ids/lookup keys intentionally are not persisted. */
data class CallBlockContactSelection(
    val displayName: String,
    val numbers: List<String>,
)

/**
 * Versioned CONTACTS payload: `v1|base64url(name):base64url(number),...|...`.
 * Original text is retained for UI/backup while matching uses sorted [PhoneKey] values.
 */
object ContactRuleCodec {
    private const val VERSION = "v1"

    fun encode(contacts: List<CallBlockContactSelection>): String {
        val encodedContacts = contacts.mapNotNull { contact ->
            val numbers = contact.numbers
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy { PhoneKey.of(it).ifEmpty { it } }
            if (numbers.isEmpty()) return@mapNotNull null
            encodePart(contact.displayName.trim()) + ":" + numbers.joinToString(",", transform = ::encodePart)
        }
        return buildString {
            append(VERSION)
            encodedContacts.forEach { append('|').append(it) }
        }
    }

    fun decode(raw: String): List<CallBlockContactSelection> {
        val tokens = raw.split('|')
        if (tokens.firstOrNull() != VERSION) return emptyList()
        return tokens.drop(1).mapNotNull(::decodeContact)
    }

    fun matchKeys(raw: String): Set<String> = decode(raw)
        .asSequence()
        .flatMap { it.numbers.asSequence() }
        .map(PhoneKey::of)
        .filter(String::isNotEmpty)
        .toSortedSet()

    fun selectedCount(raw: String): Int = decode(raw).size

    internal fun isValidPayload(raw: String): Boolean {
        val tokens = raw.split('|')
        if (tokens.firstOrNull() != VERSION || tokens.size < 2) return false
        return tokens.drop(1).all { decodeContact(it) != null } && decode(raw).isNotEmpty()
    }

    private fun decodeContact(raw: String): CallBlockContactSelection? {
        val pieces = raw.split(':', limit = 2)
        if (pieces.size != 2) return null
        val name = decodePart(pieces[0]) ?: return null
        val numberTokens = pieces[1].split(',').filter(String::isNotEmpty)
        if (numberTokens.isEmpty()) return null
        val numbers = numberTokens.map { decodePart(it) ?: return null }
            .map(String::trim)
            .filter(String::isNotEmpty)
        if (numbers.isEmpty()) return null
        return CallBlockContactSelection(name, numbers)
    }

    private fun encodePart(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String? = runCatching {
        Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
    }.getOrNull()
}

/**
 * Portable snapshot of a number selected from the system call log. Call-log row ids, timestamps,
 * SIM/account ids and provider-specific fields are deliberately omitted: they are picker metadata,
 * not part of the blocking rule and would not survive backup/restore on another device.
 */
data class CallBlockCallHistorySelection(
    val displayName: String,
    val rawNumber: String,
)

/** Snapshot of a number chosen from one of the app's user-managed categories. */
data class CallBlockCategorySelection(
    val displayName: String,
    val rawNumber: String,
)

/**
 * Versioned CALL_HISTORY payload: `v1|base64url(name):base64url(number)|...`.
 *
 * Entries are deduplicated by [PhoneKey] so domestic and international representations of the
 * same Vietnamese number produce one exact match. Canonical encoding sorts by that key, making a
 * rule signature independent from picker order and safe to merge during backup restore.
 */
object CallHistoryRuleCodec {
    private const val VERSION = "v1"

    fun encode(selections: List<CallBlockCallHistorySelection>): String {
        val canonical = canonicalSelections(selections)
        return buildString {
            append(VERSION)
            canonical.forEach { selection ->
                append('|')
                append(encodePart(selection.displayName))
                append(':')
                append(encodePart(selection.rawNumber))
            }
        }
    }

    fun decode(raw: String): List<CallBlockCallHistorySelection> {
        val tokens = raw.split('|')
        if (tokens.firstOrNull() != VERSION) return emptyList()
        return canonicalSelections(tokens.drop(1).mapNotNull(::decodeSelection))
    }

    fun matchKeys(raw: String): Set<String> = decode(raw)
        .asSequence()
        .map { PhoneKey.of(it.rawNumber) }
        .filter(String::isNotEmpty)
        .toCollection(sortedSetOf())

    fun selectedCount(raw: String): Int = matchKeys(raw).size

    /**
     * Shared picker/codec contract for a selectable telephone number. Some OEM call logs expose
     * SIP URIs or alphanumeric caller ids; extracting their embedded digits could otherwise make
     * them collide with an unrelated PSTN number through [PhoneKey]. Unicode spacing is accepted
     * because formatted numbers copied from contacts commonly contain non-ASCII space characters.
     */
    fun isSelectableNumber(rawNumber: String): Boolean {
        val raw = rawNumber.trim()
        return raw.isNotEmpty() &&
            !raw.startsWith('-') &&
            raw.any(Char::isDigit) &&
            raw.all { char ->
                char.isDigit() || char.isWhitespace() || Character.isSpaceChar(char) || char in "+-()."
            } &&
            PhoneKey.of(raw).length >= 3
    }

    internal fun isValidPayload(raw: String): Boolean {
        val tokens = raw.split('|')
        if (tokens.firstOrNull() != VERSION || tokens.size < 2) return false
        val decoded = tokens.drop(1).map { decodeSelection(it) ?: return false }
        return decoded.isNotEmpty() && decoded.all(::hasValidNumber) && matchKeys(raw).isNotEmpty()
    }

    private fun canonicalSelections(
        selections: List<CallBlockCallHistorySelection>,
    ): List<CallBlockCallHistorySelection> = selections
        .asSequence()
        .map {
            CallBlockCallHistorySelection(
                displayName = it.displayName.trim(),
                rawNumber = it.rawNumber.trim(),
            )
        }
        .filter(::hasValidNumber)
        .groupBy { PhoneKey.of(it.rawNumber) }
        .map { (key, duplicates) ->
            key to duplicates.minWith(
                compareBy<CallBlockCallHistorySelection> { it.displayName.isBlank() }
                    .thenBy { it.displayName }
                    .thenBy { it.rawNumber }
            )
        }
        .sortedBy { it.first }
        .map { it.second }

    private fun hasValidNumber(selection: CallBlockCallHistorySelection): Boolean {
        return isSelectableNumber(selection.rawNumber)
    }

    private fun decodeSelection(raw: String): CallBlockCallHistorySelection? {
        val pieces = raw.split(':', limit = 2)
        if (pieces.size != 2) return null
        val name = decodePart(pieces[0]) ?: return null
        val number = decodePart(pieces[1])?.trim().orEmpty()
        if (number.isEmpty()) return null
        return CallBlockCallHistorySelection(name.trim(), number)
    }

    private fun encodePart(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String? = runCatching {
        Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
    }.getOrNull()
}

/** Provider failures and missing permission stay UNKNOWN so an unknown-contact rule fails open. */
enum class ContactLookupStatus { IN_CONTACTS, NOT_IN_CONTACTS, UNKNOWN }

/** Network/Android caller-ID verification signal, kept Android-free for matcher unit tests. */
enum class CallerNumberVerificationStatus { UNKNOWN, NOT_VERIFIED, PASSED, FAILED }

data class CallScreeningContext(
    val number: String,
    val contactStatus: ContactLookupStatus = ContactLookupStatus.UNKNOWN,
    val isPrivateNumber: Boolean = false,
    val isVoip: Boolean = false,
    /** Caller-ID label, never a Contacts display name. Android may leave it null. */
    val callerDisplayName: String? = null,
    val sipCallerIdentity: SipCallerIdentity = SipCallerIdentity.UNKNOWN,
    val callerNumberVerificationStatus: CallerNumberVerificationStatus =
        CallerNumberVerificationStatus.UNKNOWN,
)

/**
 * One exact number in the allowlist or blocklist. Contacts, Call Log, categories and manual input
 * are only sources for creating this durable exact-number entry; [origin] never changes screening
 * logic.
 */
data class CallBlockNumberEntry(
    val id: Long,
    val action: CallBlockAction,
    val rawNumber: String,
    val phoneKey: String,
    val displayName: String,
    val origin: NumberEntryOrigin,
    val enabled: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "call_block_number_entries",
    indices = [
        Index(value = ["action", "phoneKey"], unique = true),
        Index(value = ["phoneKey"]),
        Index(value = ["action"]),
        Index(value = ["enabled"]),
    ],
)
data class CallBlockNumberEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val action: String,
    val rawNumber: String,
    val phoneKey: String,
    val displayName: String = "",
    val origin: String = NumberEntryOrigin.MANUAL.storageKey,
    val enabled: Boolean = true,
    val createdAt: Long,
)

/** Một quy tắc chặn dùng cho UI/engine, tách khỏi entity Room. */
data class CallBlockRule(
    val id: Long,
    val type: CallBlockRuleType,
    /** Giá trị nguyên gốc để hiển thị lại đúng điều người dùng đã nhập/chọn. */
    val rawValue: String,
    /** Giá trị chuẩn hoá chỉ dùng để so khớp và chống quy tắc trùng. */
    val matchValue: String,
    val enabled: Boolean,
    val createdAt: Long,
    /** Conditional ALLOW is reserved for future UI; exact allowlist entries remain the top tier. */
    val action: CallBlockAction = CallBlockAction.BLOCK,
    val scope: CallBlockScope = CallBlockScope.ALL_VISIBLE_NUMBERS,
    /** User ordering is meaningful only inside the fixed conditional-rule tier. */
    val userOrder: Int = 0,
)

/** Bản ghi Room của một quy tắc; không FK tới lịch sử để xoá quy tắc vẫn giữ được nhật ký minh bạch. */
@Entity(
    tableName = "call_block_rules",
    indices = [
        Index(value = ["action", "type", "matchValue", "scope"], unique = true),
        Index(value = ["enabled"]),
        Index(value = ["userOrder"]),
    ],
)
data class CallBlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val rawValue: String,
    val matchValue: String,
    val enabled: Boolean = true,
    val createdAt: Long,
    val action: String = CallBlockAction.BLOCK.storageKey,
    val scope: String = CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey,
    val userOrder: Int = 0,
)

/** Lý do đã khớp tại thời điểm dịch vụ sàng lọc nhận cuộc gọi. */
data class CallBlockMatch(
    val rule: CallBlockRule,
    /** Persisted history reason; normally mirrors [rule], but synthetic runtime policies override it. */
    val historyReasonType: String = rule.type.storageKey,
    val historyReasonValue: String = rule.rawValue,
    /**
     * Ephemeral guard revision used only for the final pre-Telecom race check. It is deliberately
     * excluded from Room/history/backup so a restored event can never reactivate a runtime policy.
     */
    val guardConfigSnapshot: RepeatUnknownCallerGuardConfig? = null,
    /** Final engine decision. Services must never infer it from a localized/type label. */
    val action: CallBlockAction = rule.action,
    val decisionTier: CallBlockDecisionTier = CallBlockDecisionTier.CONDITIONAL_RULE,
    /** Snapshot generation rechecked immediately before Telecom applies a destructive decision. */
    val ruleSnapshotGeneration: Long? = null,
)

enum class CallBlockDecisionTier {
    EXACT_ALLOWLIST,
    EXACT_BLOCKLIST,
    GROUP_RULE,
    CONDITIONAL_RULE,
    UNKNOWN_CALLER_POLICY,
}

/** Dedicated non-rule history reason for the no-rule repeated-unknown-caller guard. */
const val REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE = "repeat_unknown_caller_guard"

/** History-only compatibility key. v4 removes this rule type and never evaluates/restores it. */
const val LEGACY_REPEAT_UNANSWERED_REASON_TYPE = "repeat_unanswered"
private const val LEGACY_REPEAT_UNANSWERED_THRESHOLD = "5"

data class RepeatUnknownCallerGuardReason(
    val attempt: Int,
    val threshold: Int,
    val windowMinutes: Int,
)

/** Stable, locale-independent payload stored in call_block_history and portable backups. */
object RepeatUnknownCallerGuardReasonCodec {
    private const val VERSION = "v1"

    fun encode(reason: RepeatUnknownCallerGuardReason): String {
        require(reason.threshold in 2..4)
        require(reason.attempt in 1 until reason.threshold)
        require(reason.windowMinutes in 1..(24 * 60))
        return listOf(
            VERSION,
            reason.attempt,
            reason.threshold,
            reason.windowMinutes,
        ).joinToString("|")
    }

    fun decode(raw: String): RepeatUnknownCallerGuardReason? {
        val parts = raw.split('|')
        if (parts.size != 4 || parts[0] != VERSION) return null
        val attempt = parts[1].toIntOrNull() ?: return null
        val threshold = parts[2].toIntOrNull()?.takeIf { it in 2..4 } ?: return null
        val windowMinutes = parts[3].toIntOrNull()?.takeIf { it in 1..(24 * 60) } ?: return null
        if (attempt !in 1 until threshold) return null
        return RepeatUnknownCallerGuardReason(attempt, threshold, windowMinutes)
    }
}

data class DisplayCallBlockReason(
    /** Localized summaries consume stable storage keys so removed rule types can remain readable. */
    val ruleType: String,
    val ruleValue: String,
)

/** Maps dedicated persisted reasons onto existing localized UI/notification summaries. */
object CallBlockHistoryReasonCodec {
    private val unknownContactValue = SpecialCallCondition.encode(
        setOf(SpecialCallCondition.UNKNOWN_CONTACT)
    )

    fun display(storedType: String, storedValue: String): DisplayCallBlockReason? {
        CallBlockRuleType.fromStorage(storedType)?.let { type ->
            val displayValue = if (type == CallBlockRuleType.SPAM_RISK) {
                SpamRiskReasonCodec.decode(storedValue)?.let(SpamRiskReasonCodec::encode)
                    ?: CallBlockRuleMatcher.SPAM_RISK_PROFILE
            } else storedValue
            return DisplayCallBlockReason(type.storageKey, displayValue)
        }
        if (storedType == LEGACY_REPEAT_UNANSWERED_REASON_TYPE) {
            return storedValue.takeIf { it == LEGACY_REPEAT_UNANSWERED_THRESHOLD }
                ?.let { DisplayCallBlockReason(storedType, it) }
        }
        if (
            storedType == REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE &&
            RepeatUnknownCallerGuardReasonCodec.decode(storedValue) != null
        ) {
            return DisplayCallBlockReason(CallBlockRuleType.SPECIAL.storageKey, unknownContactValue)
        }
        return null
    }

    fun isSupported(storedType: String, storedValue: String): Boolean =
        display(storedType, storedValue) != null
}

/** Bản ghi Room độc lập với Call Log hệ thống cho các cuộc app đã chặn. */
@Entity(
    tableName = "call_block_history",
    indices = [
        Index(value = ["phoneKey"]),
        Index(value = ["blockedAt"]),
        // Ngăn việc cùng một callback bị ghi lặp nếu service bị gọi lại bất thường.
        Index(value = ["phoneKey", "blockedAt", "ruleType", "ruleValue"], unique = true),
    ],
)
data class CallBlockHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawNumber: String,
    val phoneKey: String,
    val blockedAt: Long,
    val ruleType: String,
    val ruleValue: String,
    val ruleScope: String = CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey,
    val consecutiveUnanswered: Int = 0,
)

/** Hàng lịch sử đã kèm tổng số lần bị chặn của cùng số điện thoại. */
data class BlockedCallHistory(
    val id: Long,
    val rawNumber: String,
    val phoneKey: String,
    val blockedAt: Long,
    val ruleType: String,
    val ruleValue: String,
    val consecutiveUnanswered: Int,
    val blockedCountForNumber: Int,
    /** Raw persisted reason. Dedicated runtime policies keep their own stable type/payload here. */
    val historyReasonType: String = ruleType,
    val historyReasonValue: String = ruleValue,
    val ruleScope: CallBlockScope = CallBlockScope.ALL_VISIBLE_NUMBERS,
)

/** Kết quả ghi lịch sử, dùng để quyết định nội dung/tần suất notification. */
data class BlockRecordResult(
    val historyId: Long,
    val rawNumber: String,
    val ruleType: String,
    val ruleValue: String,
    val totalForNumber: Int,
    val isNew: Boolean,
    /** Raw persisted reason, exposed so notifications can render dedicated policy summaries. */
    val historyReasonType: String = ruleType,
    val historyReasonValue: String = ruleValue,
    val ruleScope: CallBlockScope = CallBlockScope.ALL_VISIBLE_NUMBERS,
)

/** Kết quả lưu một quy tắc từ màn tạo/sửa để UI hiển thị thông báo chính xác. */
enum class SaveBlockRuleResult { SAVED, INVALID, DUPLICATE, FULL, NOT_FOUND }

enum class SaveNumberEntryResult {
    SAVED,
    INVALID,
    DUPLICATE,
    OPPOSITE_LIST_CONFLICT,
    FULL,
    NOT_FOUND,
}

/**
 * Chuẩn hoá + so khớp quy tắc. Tách thành object thuần Kotlin để có test độc lập với Android/Room.
 *
 * [PhoneKey.of] chỉ bỏ 0/+84 khi số đủ dài (đúng cho số hoàn chỉnh). Với prefix, người dùng thường
 * nhập đoạn ngắn như 09 hoặc +84 98 nên [prefixPatternKey] xử lý tiền tố VN sớm hơn. Suffix/contains
 * giữ nguyên mọi chữ số vì số 0 ở các vị trí đó là một phần literal của mẫu.
 */
object CallBlockRuleMatcher {
    const val SPAM_RISK_PROFILE = "app_default"

    fun normalizedValue(type: CallBlockRuleType, rawValue: String): String = when (type) {
        CallBlockRuleType.EXACT_NUMBER -> PhoneKey.of(rawValue)
        CallBlockRuleType.CONTACTS -> ContactRuleCodec.matchKeys(rawValue).joinToString(",")
        CallBlockRuleType.CALL_HISTORY -> CallHistoryRuleCodec.matchKeys(rawValue).joinToString(",")
        CallBlockRuleType.ANY -> ANY_MATCH_VALUE
        CallBlockRuleType.PREFIX -> prefixPatternKey(rawValue)
        // A leading zero in a suffix/contained fragment is literal, not the Vietnamese trunk prefix.
        // Dropping it would turn e.g. suffix "09" into the much broader suffix "9".
        CallBlockRuleType.SUFFIX,
        CallBlockRuleType.CONTAINS -> PhoneKey.digits(rawValue)
        CallBlockRuleType.LENGTH -> rawValue.trim().toIntOrNull()?.toString().orEmpty()
        CallBlockRuleType.CARRIER -> rawValue.trim()
        CallBlockRuleType.GEOGRAPHIC ->
            GeographicBlockOption.encode(GeographicBlockOption.decode(rawValue))
        CallBlockRuleType.SPECIAL -> SpecialCallCondition.canonical(rawValue)
        CallBlockRuleType.BRAND_NAME -> BrandNameRuleCodec.canonical(rawValue)
        CallBlockRuleType.SPAM_RISK -> SPAM_RISK_PROFILE
    }

    fun canonicalRawValue(type: CallBlockRuleType, rawValue: String): String = when (type) {
        CallBlockRuleType.ANY -> ANY_MATCH_VALUE
        CallBlockRuleType.LENGTH -> rawValue.trim().toIntOrNull()?.toString().orEmpty()
        CallBlockRuleType.GEOGRAPHIC ->
            GeographicBlockOption.encode(GeographicBlockOption.decode(rawValue))
        CallBlockRuleType.SPECIAL -> SpecialCallCondition.canonical(rawValue)
        CallBlockRuleType.BRAND_NAME -> BrandNameRuleCodec.canonical(rawValue)
        CallBlockRuleType.CONTACTS -> ContactRuleCodec.encode(ContactRuleCodec.decode(rawValue))
        CallBlockRuleType.CALL_HISTORY -> CallHistoryRuleCodec.encode(CallHistoryRuleCodec.decode(rawValue))
        CallBlockRuleType.SPAM_RISK -> SPAM_RISK_PROFILE
        else -> rawValue.trim()
    }

    fun isValid(type: CallBlockRuleType, rawValue: String): Boolean = when (type) {
        CallBlockRuleType.EXACT_NUMBER ->
            rawValue.trim().isNotEmpty() && !rawValue.trim().startsWith("-") && normalizedValue(type, rawValue).length >= 3
        CallBlockRuleType.CONTACTS -> {
            val contacts = ContactRuleCodec.decode(rawValue)
            ContactRuleCodec.isValidPayload(rawValue) &&
                contacts.isNotEmpty() &&
                contacts.all { contact ->
                    contact.numbers.isNotEmpty() && contact.numbers.all { PhoneKey.of(it).length >= 3 }
                } &&
                ContactRuleCodec.matchKeys(rawValue).isNotEmpty()
        }
        CallBlockRuleType.CALL_HISTORY ->
            CallHistoryRuleCodec.isValidPayload(rawValue) &&
                CallHistoryRuleCodec.decode(rawValue).all { PhoneKey.of(it.rawNumber).length >= 3 } &&
                CallHistoryRuleCodec.matchKeys(rawValue).isNotEmpty()
        CallBlockRuleType.ANY -> rawValue.isBlank() || rawValue.trim() == ANY_MATCH_VALUE
        CallBlockRuleType.PREFIX ->
            PhoneKey.digits(rawValue).length >= 2 && normalizedValue(type, rawValue).isNotEmpty()
        CallBlockRuleType.SUFFIX,
        CallBlockRuleType.CONTAINS -> normalizedValue(type, rawValue).length >= 2
        CallBlockRuleType.LENGTH -> rawValue.trim().toIntOrNull() in MIN_NUMBER_LENGTH..MAX_NUMBER_LENGTH
        CallBlockRuleType.CARRIER -> rawValue.trim() in Carrier.names
        CallBlockRuleType.GEOGRAPHIC ->
            GeographicBlockOption.isValidPayload(rawValue) && GeographicBlockOption.decode(rawValue).isNotEmpty()
        CallBlockRuleType.SPECIAL ->
            SpecialCallCondition.isValidPayload(rawValue) && SpecialCallCondition.decode(rawValue).isNotEmpty()
        CallBlockRuleType.BRAND_NAME -> BrandNameRuleCodec.isValidPayload(rawValue)
        CallBlockRuleType.SPAM_RISK -> rawValue.trim() == SPAM_RISK_PROFILE
    }

    fun matches(rule: CallBlockRule, context: CallScreeningContext): Boolean {
        if (!rule.type.supportsScope(rule.scope, rule.rawValue)) return false
        if (!rule.scope.matches(context.contactStatus)) return false
        if (rule.type == CallBlockRuleType.SPECIAL) {
            return when (SpecialCallCondition.activeSelection(rule.matchValue)) {
                SpecialCallCondition.PRIVATE_NUMBER -> context.isPrivateNumber
                SpecialCallCondition.SIP_PHONE_NUMBER ->
                    context.sipCallerIdentity.kind == SipCallerIdKind.PHONE_NUMBER
                SpecialCallCondition.SIP_TEXT_ID ->
                    context.sipCallerIdentity.kind == SipCallerIdKind.TEXT_ID
                // Retained only so immutable history/old backups remain readable.
                SpecialCallCondition.UNKNOWN_CONTACT,
                SpecialCallCondition.VOIP,
                null,
                -> false
            }
        }
        if (rule.type == CallBlockRuleType.BRAND_NAME) {
            val names = BrandNameRuleCodec.decode(rule.matchValue)
            return context.callerDisplayName?.trim()?.let(names::contains) == true
        }
        // A SIP phone user is deliberately promoted to the existing phone-number engine. Text or
        // malformed non-tel identities must never leak embedded digits into number rules.
        val matchNumber = context.sipCallerIdentity.phoneNumber ?: context.number
        if (
            context.isPrivateNumber ||
            (context.isVoip && context.sipCallerIdentity.kind != SipCallerIdKind.PHONE_NUMBER)
        ) return false
        if (rule.type == CallBlockRuleType.SPAM_RISK) return spamRiskReason(context) != null
        // ALL-visible rules are independent from provider state. Contact-scoped rules require a
        // positive/negative lookup and therefore fail open when permission/provider is unavailable.
        return matches(rule, matchNumber)
    }

    /** Source-compatible overload for the existing number-based callers/tests. */
    fun matches(rule: CallBlockRule, number: String): Boolean {
        val key = PhoneKey.of(number)
        if (key.isEmpty()) return false
        return when (rule.type) {
            CallBlockRuleType.EXACT_NUMBER -> key == rule.matchValue
            CallBlockRuleType.CONTACTS -> rule.matchValue.split(',').any { it == key }
            CallBlockRuleType.CALL_HISTORY -> rule.matchValue.split(',').any { it == key }
            CallBlockRuleType.ANY -> true
            CallBlockRuleType.PREFIX -> key.startsWith(rule.matchValue)
            CallBlockRuleType.SUFFIX -> key.endsWith(rule.matchValue)
            CallBlockRuleType.CONTAINS -> key.contains(rule.matchValue)
            CallBlockRuleType.LENGTH -> key.length == rule.matchValue.toIntOrNull()
            CallBlockRuleType.CARRIER -> Carrier.of(number) == rule.matchValue
            CallBlockRuleType.GEOGRAPHIC -> matchesGeographic(rule.matchValue, number)
            CallBlockRuleType.SPECIAL -> false
            CallBlockRuleType.BRAND_NAME -> false
            CallBlockRuleType.SPAM_RISK -> spamRiskNumberReason(number) != null
        }
    }

    fun ordered(rules: List<CallBlockRule>): List<CallBlockRule> =
        rules.sortedWith(
            compareBy<CallBlockRule> { it.userOrder }
                .thenBy { it.createdAt }
                .thenBy { it.id }
        )

    private fun matchesGeographic(rawOptions: String, rawNumber: String): Boolean {
        val options = GeographicBlockOption.decode(rawOptions)
        if (options.isEmpty()) return false

        val internationalDigits = explicitInternationalDigits(rawNumber)
        if (internationalDigits != null) {
            // +84/0084 is Vietnamese even though it uses explicit international notation.
            if (internationalDigits.startsWith(VIETNAM_CALLING_CODE)) {
                val national = internationalDigits.drop(VIETNAM_CALLING_CODE.length)
                if (national.isEmpty() || national.startsWith('0')) return false
                val domestic = "0$national"
                return options.any { option ->
                    option.kind == GeographicBlockKind.VIETNAM_PREFIX &&
                        option.domesticPrefix?.let(domestic::startsWith) == true
                }
            }

            if (GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM in options) return true
            return options.any { option ->
                option.kind == GeographicBlockKind.COUNTRY_CALLING_CODE &&
                    option.callingCode?.let { code ->
                        internationalDigits.length > code.length && internationalDigits.startsWith(code)
                    } == true
            }
        }

        val domestic = explicitVietnameseDomesticDigits(rawNumber) ?: return false
        return options.any { option ->
            option.kind == GeographicBlockKind.VIETNAM_PREFIX &&
                option.domesticPrefix?.let(domestic::startsWith) == true
        }
    }

    /**
     * App-maintained offline risk profile. Prefixes are user-requested risk filters, not a claim
     * that every number in a legitimate allocated range is fraudulent. An "unrecognized" mobile
     * is strictly a ten-digit Vietnamese mobile-shaped number in an allocated mobile namespace
     * whose three/four-digit prefix is absent from the app's current [Carrier] table.
     */
    fun spamRiskReason(context: CallScreeningContext): SpamRiskReason? {
        if (context.isPrivateNumber || context.isVoip) return null
        if (context.callerNumberVerificationStatus == CallerNumberVerificationStatus.FAILED) {
            return SpamRiskReason(SpamRiskReasonKind.VERIFICATION_FAILED)
        }
        return spamRiskNumberReason(context.number)
    }

    private fun spamRiskNumberReason(rawNumber: String): SpamRiskReason? {
        val domestic = explicitVietnameseNumber(rawNumber) ?: return null
        SPAM_RISK_PREFIX_LENGTHS.entries.firstOrNull { (prefix, expectedDigits) ->
            domestic.length == expectedDigits && domestic.startsWith(prefix)
        }?.key?.let { prefix ->
            return SpamRiskReason(SpamRiskReasonKind.PREFIX, prefix)
        }
        val isUnknownMobile = domestic.length == VIETNAM_MOBILE_DIGITS &&
            domestic.getOrNull(1)?.let(VIETNAM_MOBILE_NAMESPACE_DIGITS::contains) == true &&
            Carrier.of(domestic) == null
        return if (isUnknownMobile) {
            SpamRiskReason(SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX, domestic.take(3))
        } else null
    }

    private fun explicitVietnameseNumber(rawNumber: String): String? {
        val internationalDigits = explicitInternationalDigits(rawNumber)
        if (internationalDigits != null) {
            if (!internationalDigits.startsWith(VIETNAM_CALLING_CODE)) return null
            val national = internationalDigits.drop(VIETNAM_CALLING_CODE.length)
            if (national.isEmpty() || national.startsWith('0')) return null
            return "0$national"
        }
        return explicitVietnameseDomesticDigits(rawNumber)
    }

    /**
     * Returns digits after a leading `+` or international access prefix `00` only. Bare `86...`
     * remains ambiguous and fails open. E.164-style numbers are limited to 7..15 digits and a
     * country calling code never starts with zero.
     */
    private fun explicitInternationalDigits(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || !value.all(::isDialStringCharacter)) return null
        val digits = PhoneKey.digits(value)
        val international = when {
            value.startsWith('+') && value.drop(1).none { it == '+' } -> digits
            '+' !in value && digits.startsWith("00") -> digits.drop(2)
            else -> return null
        }
        return international.takeIf {
            it.length in MIN_INTERNATIONAL_DIGITS..MAX_INTERNATIONAL_DIGITS && !it.startsWith('0')
        }
    }

    /** Domestic matching is deliberately strict so a bare international-looking number fails open. */
    private fun explicitVietnameseDomesticDigits(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || '+' in value || !value.all(::isDialStringCharacter)) return null
        val digits = PhoneKey.digits(value)
        return digits.takeIf {
            it.length in MIN_VIETNAM_DOMESTIC_DIGITS..MAX_VIETNAM_DOMESTIC_DIGITS &&
                it.startsWith('0') && !it.startsWith("00")
        }
    }

    private fun isDialStringCharacter(char: Char): Boolean =
        char in '0'..'9' || char.isWhitespace() || char in "+-()."

    private fun prefixPatternKey(raw: String): String {
        var digits = PhoneKey.digits(raw)
        if (digits.length > 4 && digits.startsWith("0084")) digits = digits.drop(4)
        else if (digits.length >= 3 && digits.startsWith("84")) digits = digits.drop(2)
        if (digits.length >= 2 && digits.startsWith("0")) digits = digits.drop(1)
        return digits
    }

    private const val VIETNAM_CALLING_CODE = "84"
    private const val MIN_INTERNATIONAL_DIGITS = 7
    private const val MAX_INTERNATIONAL_DIGITS = 15
    private const val MIN_VIETNAM_DOMESTIC_DIGITS = 7
    private const val MAX_VIETNAM_DOMESTIC_DIGITS = 15
    private const val VIETNAM_MOBILE_DIGITS = 10
    private const val ANY_MATCH_VALUE = "any"
    private const val MIN_NUMBER_LENGTH = 1
    private const val MAX_NUMBER_LENGTH = 32
    private val SPAM_RISK_PREFIX_LENGTHS = linkedMapOf(
        "023" to 11,
        "024" to 11,
        "028" to 11,
        "022" to 11,
        "059" to 10,
        "099" to 10,
    )
    private val VIETNAM_MOBILE_NAMESPACE_DIGITS = setOf('3', '5', '7', '8', '9')
}

internal fun CallBlockRuleEntity.toModel(): CallBlockRule? =
    CallBlockRuleType.fromStorage(type)?.let { parsedType ->
        val parsedAction = CallBlockAction.fromStorage(action) ?: return null
        if (!parsedType.supportsAction(parsedAction)) return null
        val parsedScope = CallBlockScope.fromStorage(scope) ?: return null
        if (!parsedType.supportsScope(parsedScope, rawValue)) return null
        if (
            parsedType in setOf(CallBlockRuleType.SPECIAL, CallBlockRuleType.BRAND_NAME) &&
            !CallBlockRuleMatcher.isValid(parsedType, rawValue)
        ) {
            return null
        }
        CallBlockRule(
            id = id,
            type = parsedType,
            rawValue = rawValue,
            matchValue = matchValue,
            enabled = enabled,
            createdAt = createdAt,
            action = parsedAction,
            scope = parsedScope,
            userOrder = userOrder,
        )
    }

internal fun CallBlockNumberEntryEntity.toModel(): CallBlockNumberEntry? {
    val parsedAction = CallBlockAction.fromStorage(action) ?: return null
    val parsedOrigin = NumberEntryOrigin.fromStorage(origin) ?: return null
    return CallBlockNumberEntry(
        id = id,
        action = parsedAction,
        rawNumber = rawNumber,
        phoneKey = phoneKey,
        displayName = displayName,
        origin = parsedOrigin,
        enabled = enabled,
        createdAt = createdAt,
    )
}
