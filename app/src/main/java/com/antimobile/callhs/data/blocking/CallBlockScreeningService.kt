package com.antimobile.callhs.data.blocking

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.telecom.TelecomManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.antimobile.callhs.data.outgoing.OutgoingCallAlertDispatcher
import com.antimobile.callhs.data.outgoing.OutgoingCallEventSource
import com.antimobile.callhs.data.outgoing.OutgoingCallRole
import com.antimobile.callhs.i18n.LanguageSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Điểm chặn THẬT của ứng dụng. Android chỉ bind service này khi người dùng đã chọn CallHS cho
 * ROLE_CALL_SCREENING; không phải default dialer và không thay thế ứng dụng gọi điện của máy.
 */
class CallBlockScreeningService : CallScreeningService() {

    /** Provider/Room work must never occupy the main thread that receives Telecom callbacks. */
    private val screeningScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** A dedicated thread prevents CPU/IO coroutine-pool saturation from delaying the 4s fallback. */
    private val watchdogExecutor = (Executors.newScheduledThreadPool(1) { task ->
        Thread(task, "CallHS-screening-watchdog").apply { isDaemon = true }
    } as ScheduledThreadPoolExecutor).apply {
        removeOnCancelPolicy = true
        executeExistingDelayedTasksAfterShutdownPolicy = false
    }

    override fun onScreenCall(callDetails: Call.Details) {
        // Call.Details của screening không cam kết accountHandle. Chỉ dùng làm fallback cho người
        // dùng cũ chưa cấp ROLE_CALL_REDIRECTION; response outgoing vốn bị Android bỏ qua.
        if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
            if (!OutgoingCallRole.isHeld(applicationContext)) {
                OutgoingCallAlertDispatcher.onOutgoingCall(
                    context = applicationContext,
                    handle = callDetails.handle,
                    phoneAccount = null,
                    createdAtMillis = callDetails.creationTimeMillis,
                    source = OutgoingCallEventSource.SCREENING_FALLBACK,
                )
            }
            return
        }
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val startedAt = SystemClock.elapsedRealtime()
        val appContext = applicationContext
        val callCreatedAt = callDetails.creationTimeMillis
        val callbackAge = callCreatedAt.takeIf { it > 0L }
            ?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) }
        if (callbackAge != null && callbackAge >= SLOW_CALLBACK_AGE_MS) {
            Log.w(LOG_TAG, "Telecom delivered screening callback for a ${callbackAge} ms old call")
        }
        // Schedule the deadline before touching SharedPreferences, Room or a ContentProvider. All
        // of them can block during process cold start or while another operation owns a DB/provider lock.
        val responseClaimed = AtomicBoolean(false)
        val fallback = runCatching {
            watchdogExecutor.schedule({
                if (responseClaimed.compareAndSet(false, true)) {
                    Log.w(LOG_TAG, "Screening deadline reached; allowing call after ${elapsed(startedAt)} ms")
                    runCatching { allow(callDetails) }
                        .onFailure { error -> Log.e(LOG_TAG, "Unable to invoke deadline response", error) }
                }
            }, RESPONSE_DEADLINE_MS, TimeUnit.MILLISECONDS)
        }.getOrElse { error ->
            Log.e(LOG_TAG, "Unable to schedule screening deadline; allowing call immediately", error)
            runCatching { allow(callDetails) }
                .onFailure { responseError ->
                    Log.e(LOG_TAG, "Unable to invoke emergency allow response", responseError)
                }
            return
        }

        screeningScope.launch {
            // These first reads may synchronously load preference XML in a cold process, hence IO.
            val settings = runCatching {
                LanguageSettings.init(appContext)
                CallBlockSettings.isBlockingEnabled(appContext) to CallBlockSettings.blockMethod(appContext)
            }.onFailure { error ->
                Log.e(LOG_TAG, "Unable to read screening settings; call will be allowed", error)
            }.getOrNull()
            if (settings == null || !settings.first || settings.second == CallBlockMethod.ALLOW) {
                respondOnce(responseClaimed, fallback, startedAt) { allow(callDetails) }
                return@launch
            }

            val handle = callDetails.handle
            // Only encoded-form literal delimiters are trusted; decoded opaque structure is
            // ambiguous and fails open.
            val address = IncomingCallAddressParser.parse(
                scheme = handle?.scheme,
                encodedSchemeSpecificPart = handle?.encodedSchemeSpecificPart,
                decodedSchemeSpecificPart = handle?.schemeSpecificPart,
                encodedFragment = handle?.encodedFragment,
            )
            val number = address.screeningAddress
            // AOSP normally omits hidden/non-tel calls from this service. These flags are best-effort for
            // OEMs that still deliver them; the standard Android path must not be advertised as support.
            val isPrivateNumber =
                callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED ||
                    address.rawAddress.isBlank()

            val lookup = try {
                val repository = CallBlockRepository(appContext)
                repository to repository.findMatch(
                    number = number,
                    isPrivateNumber = isPrivateNumber,
                    isVoip = address.isNonTelHandle,
                    sipCallerIdentity = address.sipCallerIdentity,
                    callCreatedAt = callCreatedAt,
                    callerNumberVerificationStatus = callerNumberVerificationStatus(callDetails),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e(LOG_TAG, "Screening lookup failed; call will be allowed", error)
                null
            }
            if (lookup?.second == null) {
                respondOnce(responseClaimed, fallback, startedAt) { allow(callDetails) }
                return@launch
            }
            val repo = lookup.first
            val match = lookup.second ?: return@launch

            // ALLOW is a first-class engine result (exact allowlist or a conditional allow rule),
            // not a blocking method. It ends evaluation immediately and never writes history.
            if (match.action == CallBlockAction.ALLOW) {
                respondOnce(responseClaimed, fallback, startedAt) { allow(callDetails) }
                return@launch
            }

            // A settings change may race the Room/provider lookup. Re-read the authoritative values
            // immediately before claiming the response so OFF/pause/ALLOW cannot use a stale block,
            // a changed screening method is applied to this very call, and a guard decision from a
            // previous enabled/threshold/window session cannot survive a last-moment toggle.
            val policy = runCatching {
                val snapshotStillCurrent = match.ruleSnapshotGeneration?.let {
                    CallBlockRuleSnapshotStore.isGenerationCurrent(it)
                } ?: true
                val guardStillCurrent = match.guardConfigSnapshot?.let { expected ->
                    CallBlockSettings.repeatUnknownCallerGuardConfig(appContext) == expected
                } ?: true
                if (!snapshotStillCurrent || !guardStillCurrent) {
                    null
                } else {
                    CallResponsePolicy.forActiveBlocking(
                        enabled = CallBlockSettings.isBlockingEnabled(appContext),
                        method = CallBlockSettings.blockMethod(appContext),
                    )
                }
            }.onFailure { error ->
                Log.e(LOG_TAG, "Unable to re-read screening settings; call will be allowed", error)
            }.getOrNull()
            if (policy == null) {
                respondOnce(responseClaimed, fallback, startedAt) { allow(callDetails) }
                return@launch
            }

            // The watchdog already allowed the call; a late result must not send another response
            // and, critically, must not be recorded/notified as a successful block.
            if (!responseClaimed.compareAndSet(false, true)) {
                Log.w(LOG_TAG, "Discarding late matching result after ${elapsed(startedAt)} ms")
                return@launch
            }
            fallback.cancel(false)

            // Phản hồi Telecom ngay khi đã có kết quả (trước khi ghi lịch sử/notification) để
            // luôn nằm an toàn dưới thời hạn 5 giây của API.
            val responseInvoked = runCatching {
                respondToCall(
                    callDetails,
                    CallResponse.Builder()
                        .setDisallowCall(policy.disallowCall)
                        .setRejectCall(policy.rejectCall)
                        .setSilenceCall(policy.silenceCall)
                        // App sẽ gửi notification có ngữ cảnh/nhịp do người dùng chọn.
                        .setSkipNotification(policy.skipNotification)
                        .build(),
                )
            }.onFailure { error ->
                Log.e(LOG_TAG, "Unable to invoke Telecom screening response", error)
            }.isSuccess

            // SILENCE_ONLY and ALLOW are not blocked events and must not inflate history/counts or
            // emit a notification that claims the call was blocked.
            if (!responseInvoked || !policy.blocksCall) return@launch

            // Post the Android-owned heads-up immediately after Telecom has the blocking response.
            // Waiting for a Room transaction first used to create a race where process reclaim lost
            // the alert entirely. The durable history result below updates this same ID silently.
            val notificationEventId = notificationEventId(address.historyIdentity, callCreatedAt)
            val immediateNotification = repo.previewBlockedCall(address.historyIdentity, match, notificationEventId)
                ?.let { preview ->
                    runCatching {
                        CallBlockNotifier.notifyBlocked(
                            context = appContext,
                            result = preview,
                            notificationEventId = notificationEventId,
                        )
                    }.onFailure { error ->
                        Log.e(LOG_TAG, "Unable to post immediate blocked-call notification", error)
                    }.getOrNull()
                }

            // Telecom normally unbinds immediately after respondToCall. A service-owned Job would
            // therefore be cancelled by onDestroy before Room history/notification completes.
            // Keep only this post-response work in the process scope; screening itself stays scoped
            // to the bound service and its five-second contract.
            postResponseScope.launch {
                // The process can be started directly for screening without MainActivity. Load the
                // persisted language here, outside the response deadline, before creating app text.
                runCatching { LanguageSettings.init(appContext) }
                    .onFailure { error -> Log.e(LOG_TAG, "Unable to load notification language", error) }
                val result = runCatching {
                    repo.recordBlockedCall(address.historyIdentity, match, blockedAt = callCreatedAt)
                }
                    .onFailure { error -> Log.e(LOG_TAG, "Unable to record blocked call", error) }
                    .getOrNull()
                if (result != null) {
                    runCatching {
                        // Same notification ID + setOnlyAlertOnce updates count/reason without a
                        // second sound. If the immediate post was disabled, this remains disabled.
                        if (
                            immediateNotification == CallBlockNotifier.DeliveryResult.POSTED ||
                            immediateNotification == CallBlockNotifier.DeliveryResult.FAILED
                        ) {
                            CallBlockNotifier.notifyBlocked(
                                appContext,
                                result,
                                notificationEventId = notificationEventId,
                            )
                        }
                    }
                        .onFailure { error -> Log.e(LOG_TAG, "Unable to post blocked-call notification", error) }
                }
            }
        }
    }

    private inline fun respondOnce(
        responseClaimed: AtomicBoolean,
        fallback: ScheduledFuture<*>,
        startedAt: Long,
        response: () -> Unit,
    ) {
        if (!responseClaimed.compareAndSet(false, true)) return
        fallback.cancel(false)
        runCatching(response).onFailure { error ->
            Log.e(LOG_TAG, "Unable to invoke allow response after ${elapsed(startedAt)} ms", error)
        }
    }

    private fun allow(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder().build())
    }

    private fun notificationEventId(rawIdentity: String, callCreatedAt: Long): Long {
        val eventTime = callCreatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val identityBits = (BlockedCallerIdentity.key(rawIdentity) ?: rawIdentity.trim())
            .hashCode()
            .toLong() shl Int.SIZE_BITS
        return (eventTime xor identityBits).takeIf { it != 0L } ?: eventTime.coerceAtLeast(1L)
    }

    /** API 29 has no verification verdict. NOT_VERIFIED is deliberately distinct from FAILED. */
    private fun callerNumberVerificationStatus(
        callDetails: Call.Details,
    ): CallerNumberVerificationStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return CallerNumberVerificationStatus.UNKNOWN
        }
        return when (callDetails.callerNumberVerificationStatus) {
            Connection.VERIFICATION_STATUS_FAILED -> CallerNumberVerificationStatus.FAILED
            Connection.VERIFICATION_STATUS_PASSED -> CallerNumberVerificationStatus.PASSED
            Connection.VERIFICATION_STATUS_NOT_VERIFIED -> CallerNumberVerificationStatus.NOT_VERIFIED
            else -> CallerNumberVerificationStatus.UNKNOWN
        }
    }

    override fun onDestroy() {
        screeningScope.cancel()
        watchdogExecutor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        private const val LOG_TAG = "CallBlockScreening"
        /** Leaves one second before the API's five-second onScreenCall response deadline. */
        const val RESPONSE_DEADLINE_MS = 4_000L
        const val SLOW_CALLBACK_AGE_MS = 1_000L

        /** Not cancelled when Telecom unbinds the short-lived screening service. */
        val postResponseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun elapsed(startedAt: Long): Long = SystemClock.elapsedRealtime() - startedAt
    }
}
