package com.antimobile.mcas.data.blocking

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Compact, versioned copy of the enabled rules used by the time-critical screening path.
 *
 * Room remains the source of truth. This snapshot exists so a cold [CallScreeningService] process
 * does not have to open/migrate/wait for the Room database before it can reject an exact/pattern
 * match. The payload is validated and canonical match values are recomputed before it is trusted.
 */
internal object CallBlockRuleSnapshotCodec {
    /**
     * v3 invalidates snapshots from before the repeat-unanswered removal. Room remains authoritative;
     * current validation also rejects the retired unknown-contact SPECIAL shape, forcing a rebuild
     * when necessary.
     */
    private const val VERSION = "v3"
    private const val FIELD_SEPARATOR = ':'

    fun encode(rules: List<CallBlockRule>): String = buildString {
        append(VERSION)
        CallBlockRuleMatcher.ordered(rules.filter(CallBlockRule::enabled)).forEach { rule ->
            append('|')
            append(rule.id)
            append(FIELD_SEPARATOR)
            append(rule.type.storageKey)
            append(FIELD_SEPARATOR)
            append(rule.createdAt)
            append(FIELD_SEPARATOR)
            append(rule.action.storageKey)
            append(FIELD_SEPARATOR)
            append(rule.scope.storageKey)
            append(FIELD_SEPARATOR)
            append(rule.userOrder)
            append(FIELD_SEPARATOR)
            append(encodePart(rule.rawValue))
            append(FIELD_SEPARATOR)
            append(encodePart(rule.matchValue))
        }
    }

    /** Returns null for a partial, corrupt, non-canonical or future-version payload. */
    fun decode(payload: String): List<CallBlockRule>? {
        val tokens = payload.split('|')
        if (tokens.firstOrNull() != VERSION || tokens.size - 1 > CallBlockRepository.MAX_RULES) {
            return null
        }

        val signatures = HashSet<String>()
        val ids = HashSet<Long>()
        val rules = ArrayList<CallBlockRule>(tokens.size - 1)
        for (token in tokens.drop(1)) {
            val fields = token.split(FIELD_SEPARATOR, limit = 8)
            if (fields.size != 8) return null
            val id = fields[0].toLongOrNull()?.takeIf { it >= 0L } ?: return null
            val type = CallBlockRuleType.fromStorage(fields[1]) ?: return null
            val createdAt = fields[2].toLongOrNull()?.takeIf { it >= 0L } ?: return null
            val action = CallBlockAction.fromStorage(fields[3]) ?: return null
            val scope = CallBlockScope.fromStorage(fields[4]) ?: return null
            val userOrder = fields[5].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val rawValue = decodePart(fields[6]) ?: return null
            val storedMatch = decodePart(fields[7]) ?: return null

            if (
                !type.supportsAction(action) ||
                !type.supportsScope(scope, rawValue) ||
                !CallBlockRuleMatcher.isValid(type, rawValue)
            ) return null
            val canonicalRaw = CallBlockRuleMatcher.canonicalRawValue(type, rawValue)
            val canonicalMatch = CallBlockRuleMatcher.normalizedValue(type, canonicalRaw)
            if (storedMatch != canonicalMatch) return null
            val signature = listOf(
                action.storageKey,
                type.storageKey,
                canonicalMatch,
                scope.storageKey,
            ).joinToString("\u0000")
            if (!ids.add(id) || !signatures.add(signature)) return null

            rules += CallBlockRule(
                id = id,
                type = type,
                rawValue = canonicalRaw,
                matchValue = canonicalMatch,
                enabled = true,
                createdAt = createdAt,
                action = action,
                scope = scope,
                userOrder = userOrder,
            )
        }
        return CallBlockRuleMatcher.ordered(rules)
    }

    private fun encodePart(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String? = runCatching {
        Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
    }.getOrNull()
}

/**
 * Process cache plus an atomic device-protected SharedPreferences snapshot.
 *
 * A mutation first commits `trusted=false`, then changes Room, and finally publishes the complete
 * replacement. Therefore a process death between the Room write and cache refresh can only cause a
 * Room fallback, never continued use of a stale rule set. The generation token prevents an older
 * concurrent Room read from re-publishing itself after a newer mutation marked the cache dirty.
 */
internal object CallBlockRuleSnapshotStore {
    private const val PREFS = "call_block_screening_snapshot"
    private const val KEY_TRUSTED = "trusted"
    private const val KEY_PAYLOAD = "payload"
    private const val LOG_TAG = "CallBlockRuleSnapshot"

    private val lock = Any()
    private val mutationMutex = Mutex()
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var generation = 0L
    private var memoryState: MemoryState = MemoryState.Uninitialized

    private sealed interface MemoryState {
        data object Uninitialized : MemoryState
        data object Unavailable : MemoryState
        /** Previous immutable rules remain effective until the mutation publishes its replacement. */
        data class Dirty(val previousRules: List<CallBlockRule>?) : MemoryState
        data class Available(val rules: List<CallBlockRule>) : MemoryState
    }

    /** Serializes precheck -> dirty marker -> Room commit -> authoritative snapshot publication. */
    suspend fun <T> withRuleMutation(block: suspend () -> T): T {
        mutationMutex.lock()
        return try {
            block()
        } finally {
            // Cancellation can be delivered after markDirty committed but before the token is
            // returned/refreshed. Keep the durable trusted=false marker and let the next read reload
            // Room instead of leaving this process permanently stuck in Dirty.
            synchronized(lock) {
                if (memoryState is MemoryState.Dirty) memoryState = MemoryState.Unavailable
            }
            mutationMutex.unlock()
        }
    }

    /**
     * Linearizable screening read. A mutation either completes before this snapshot is captured or
     * starts after it; it cannot mark dirty between the barrier and [rulesOrNull].
     */
    suspend fun <T> withConsistentRuleRead(block: suspend () -> T): T {
        mutationMutex.lock()
        return try {
            block()
        } finally {
            mutationMutex.unlock()
        }
    }

    /** A valid empty list is distinct from null (missing/dirty/corrupt snapshot). */
    fun rulesOrNull(context: Context): List<CallBlockRule>? = synchronized(lock) {
        when (val state = memoryState) {
            is MemoryState.Available -> state.rules
            is MemoryState.Dirty -> state.previousRules
            MemoryState.Unavailable -> null
            MemoryState.Uninitialized -> {
                val prefs = prefs(context)
                val decoded = if (prefs.getBoolean(KEY_TRUSTED, false)) {
                    prefs.getString(KEY_PAYLOAD, null)?.let(CallBlockRuleSnapshotCodec::decode)
                } else {
                    null
                }
                memoryState = decoded?.let(MemoryState::Available) ?: MemoryState.Unavailable
                if (decoded == null && prefs.getBoolean(KEY_TRUSTED, false)) {
                    Log.w(LOG_TAG, "Ignoring corrupt screening rule snapshot; Room fallback required")
                }
                decoded
            }
        }
    }

    /** Lock-only hot path; null means the caller must load persisted state/Room consistently. */
    fun inMemoryRulesOrNull(): List<CallBlockRule>? = synchronized(lock) {
        when (val state = memoryState) {
            is MemoryState.Available -> state.rules
            is MemoryState.Dirty -> state.previousRules
            MemoryState.Unavailable,
            MemoryState.Uninitialized,
            -> null
        }
    }

    /** Called only while [withConsistentRuleRead] owns the coordinator lock. */
    fun generationForConsistentRead(): Long = synchronized(lock) {
        // Defensive recovery for an orphaned in-memory lease; persisted trusted=false remains safe.
        if (memoryState is MemoryState.Dirty) memoryState = MemoryState.Unavailable
        generation
    }

    /** Final pre-Telecom check preventing a deleted/moved/edited decision from acting stale. */
    fun isGenerationCurrent(expectedGeneration: Long): Boolean = synchronized(lock) {
        generation == expectedGeneration && memoryState !is MemoryState.Dirty
    }

    /** Lets a later screening read retry Room if the mutation's immediate refresh failed. */
    fun abandonMutationIfOwned(expectedGeneration: Long) = synchronized(lock) {
        if (generation == expectedGeneration && memoryState is MemoryState.Dirty) {
            memoryState = MemoryState.Unavailable
        }
    }

    suspend fun markDirty(context: Context): Long = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val previousState = memoryState
            generation++
            memoryState = MemoryState.Dirty(
                previousRules = when (previousState) {
                    is MemoryState.Available -> previousState.rules
                    is MemoryState.Dirty -> previousState.previousRules
                    else -> null
                }
            )
            val commitResult = runCatching {
                prefs(context).edit()
                    .putBoolean(KEY_TRUSTED, false)
                    .remove(KEY_PAYLOAD)
                    .commit()
            }
            val committed = commitResult.getOrDefault(false)
            if (!committed) {
                // Room has not been touched yet. Keep the previous durable snapshot authoritative and
                // abort the mutation; otherwise a process death could resurrect stale trusted rules.
                memoryState = when (previousState) {
                    is MemoryState.Available -> previousState
                    else -> MemoryState.Unavailable
                }
                Log.e(LOG_TAG, "Unable to mark screening rule snapshot untrusted; aborting mutation")
                commitResult.exceptionOrNull()?.let { Log.e(LOG_TAG, "Dirty-marker write failed", it) }
                throw IllegalStateException("Unable to invalidate screening rule snapshot")
            }
            generation
        }
    }

    suspend fun publishIfUnchanged(
        context: Context,
        expectedGeneration: Long,
        rules: List<CallBlockRule>,
    ): Boolean = withContext(Dispatchers.IO) {
        val ordered = CallBlockRuleMatcher.ordered(rules.filter(CallBlockRule::enabled))
        val payload = CallBlockRuleSnapshotCodec.encode(ordered)
        synchronized(lock) {
            if (generation != expectedGeneration) return@synchronized false
            val commitResult = runCatching {
                prefs(context).edit()
                    .putString(KEY_PAYLOAD, payload)
                    .putBoolean(KEY_TRUSTED, true)
                    .commit()
            }
            val committed = commitResult.getOrDefault(false)
            // Publication is atomic with the trusted durable commit. On failure, force a Room
            // bootstrap instead of exposing a state that was not published successfully.
            memoryState = if (committed) MemoryState.Available(ordered) else MemoryState.Unavailable
            if (!committed) Log.e(LOG_TAG, "Unable to persist screening rule snapshot")
            commitResult.exceptionOrNull()?.let { Log.e(LOG_TAG, "Snapshot write failed", it) }
            committed
        }
    }

    /**
     * Room fallback must not spend the screening deadline serializing/committing a potentially large
     * contacts/history rule. Install the authoritative list in RAM now and persist it in process scope.
     */
    fun publishBootstrapAsync(
        context: Context,
        expectedGeneration: Long,
        rules: List<CallBlockRule>,
    ) {
        val ordered = CallBlockRuleMatcher.ordered(rules.filter(CallBlockRule::enabled))
        val accepted = synchronized(lock) {
            if (generation != expectedGeneration || memoryState is MemoryState.Dirty) {
                false
            } else {
                memoryState = MemoryState.Available(ordered)
                true
            }
        }
        if (!accepted) return
        persistenceScope.launch {
            publishIfUnchanged(context, expectedGeneration, ordered)
        }
    }

    // Pure in-memory hooks keep concurrency tests independent from Android SharedPreferences.
    @VisibleForTesting
    internal fun installMemoryForTest(rules: List<CallBlockRule>) = synchronized(lock) {
        generation = 0L
        memoryState = MemoryState.Available(CallBlockRuleMatcher.ordered(rules))
    }

    @VisibleForTesting
    internal fun beginDirtyForTest(): Long = synchronized(lock) {
        val previous = (memoryState as? MemoryState.Available)?.rules
        generation++
        memoryState = MemoryState.Dirty(previous)
        generation
    }

    @VisibleForTesting
    internal fun publishMemoryForTest(expectedGeneration: Long, rules: List<CallBlockRule>): Boolean =
        synchronized(lock) {
            if (generation != expectedGeneration) return@synchronized false
            memoryState = MemoryState.Available(CallBlockRuleMatcher.ordered(rules))
            true
        }

    @VisibleForTesting
    internal fun resetMemoryForTest() = synchronized(lock) {
        generation = 0L
        memoryState = MemoryState.Uninitialized
    }

    private fun prefs(context: Context) = context.applicationContext
        .createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
