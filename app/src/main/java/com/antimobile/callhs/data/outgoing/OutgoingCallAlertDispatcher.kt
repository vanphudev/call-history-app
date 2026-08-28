package com.antimobile.callhs.data.outgoing

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.telecom.PhoneAccountHandle
import android.telephony.TelephonyManager
import android.util.Log
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.i18n.LanguageSettings
import com.antimobile.callhs.ui.theme.ThemeSettings
import com.antimobile.callhs.util.Carrier
import com.antimobile.callhs.util.PhoneKey
import com.antimobile.callhs.util.SimInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Phân tích và hiển thị sau khi Telecom báo một cuộc gọi đi qua redirection hoặc fallback screening. */
object OutgoingCallAlertDispatcher {
    private const val LOG_TAG = "OutgoingCallAlert"
    private const val DUPLICATE_WINDOW_MS = 2_500L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val duplicateLock = Any()
    private val recentCallbacks = LinkedHashMap<String, Long>()

    fun onOutgoingCall(
        context: Context,
        handle: Uri?,
        phoneAccount: PhoneAccountHandle?,
        createdAtMillis: Long,
        source: OutgoingCallEventSource,
    ) {
        val appContext = context.applicationContext
        // Keep the caller (especially Telecom/BroadcastReceiver main threads) enqueue-only. Even
        // parsing a URI or the first SharedPreferences read can stall during a cold process start.
        scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val number = outgoingNumber(handle) ?: return@launch
            val config = runCatching { OutgoingCallSettings.read(appContext) }
                .onFailure { Log.e(LOG_TAG, "Unable to read outgoing-call settings", it) }
                .getOrNull()
                ?: return@launch
            if (!config.enabled || !config.hasEnabledCondition) return@launch
            if (!claimCallback(number, startedAt)) {
                Log.i(LOG_TAG, "Ignoring duplicate callback from $source")
                return@launch
            }

            runCatching { LanguageSettings.init(appContext) }
                .onFailure { Log.w(LOG_TAG, "Unable to load app language", it) }

            // Hai nguồn độc lập được đọc song song. Không chạm Room nếu người dùng không bật cảnh
            // báo theo danh sách; không đọc SubscriptionManager nếu không bật cảnh báo ngoại mạng.
            val membershipDeferred = async {
                if (!config.notifyBlocklist && !config.notifyAllowlist) return@async OutgoingNumberList.NONE
                runCatching {
                    when (CallBlockRepository(appContext).findEnabledExactNumberEntry(number)?.action) {
                        CallBlockAction.BLOCK -> OutgoingNumberList.BLOCKLIST
                        CallBlockAction.ALLOW -> OutgoingNumberList.ALLOWLIST
                        null -> OutgoingNumberList.NONE
                    }
                }.onFailure { Log.e(LOG_TAG, "Unable to read exact number lists", it) }
                    .getOrDefault(OutgoingNumberList.NONE)
            }
            val simCarrierDeferred = async {
                if (!config.notifyOffNetwork) return@async null
                currentSimCarrier(appContext, phoneAccount)
            }

            val membership = membershipDeferred.await()
            val simCarrier = simCarrierDeferred.await()
            val targetCarrier = if (config.notifyOffNetwork) Carrier.of(number) else null
            val decision = OutgoingCallAlertPolicy.evaluate(
                config = config,
                membership = membership,
                simCarrier = simCarrier,
                targetCarrier = targetCarrier,
            )
            if (!decision.shouldAlert) {
                Log.i(
                    LOG_TAG,
                    "No alert from $source after ${SystemClock.elapsedRealtime() - startedAt} ms; " +
                        "account=${phoneAccount != null}, sim=$simCarrier, target=$targetCarrier, list=$membership",
                )
                return@launch
            }

            val event = OutgoingCallAlertEvent(
                number = number,
                simCarrier = simCarrier,
                targetCarrier = targetCarrier,
                membership = membership,
                reasons = decision.reasons,
                createdAtMillis = createdAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )
            val overlayShown = if (config.presentation == OutgoingCallPresentation.OVERLAY) {
                withContext(Dispatchers.Main.immediate) {
                    runCatching {
                        ThemeSettings.init(appContext)
                        OutgoingCallOverlay.show(appContext, event)
                    }.onFailure { Log.e(LOG_TAG, "Unable to show outgoing-call overlay", it) }
                        .getOrDefault(false)
                }
            } else {
                false
            }
            if (!overlayShown) {
                val posted = runCatching { OutgoingCallNotifier.notify(appContext, event) }
                    .onFailure { Log.e(LOG_TAG, "Unable to show outgoing-call notification", it) }
                    .getOrDefault(false)
                Log.i(
                    LOG_TAG,
                    "Alert from $source completed in ${SystemClock.elapsedRealtime() - startedAt} ms; " +
                        "notification=$posted reasons=${decision.reasons}",
                )
            } else {
                Log.i(
                    LOG_TAG,
                    "Alert from $source completed in ${SystemClock.elapsedRealtime() - startedAt} ms; " +
                        "overlay=true reasons=${decision.reasons}",
                )
            }
        }
    }

    private fun claimCallback(number: String, nowElapsed: Long): Boolean = synchronized(duplicateLock) {
        recentCallbacks.entries.removeAll { nowElapsed - it.value > DUPLICATE_WINDOW_MS }
        val key = PhoneKey.of(number)
        val previous = recentCallbacks[key]
        recentCallbacks[key] = nowElapsed
        previous == null || nowElapsed - previous > DUPLICATE_WINDOW_MS
    }

    private fun outgoingNumber(handle: Uri?): String? {
        if (!handle?.scheme.equals("tel", ignoreCase = true)) return null
        val raw = handle?.schemeSpecificPart?.trim().orEmpty()
        // USSD/MMI is sent through tel: too, but is not a normal outgoing telephone number.
        if (raw.isBlank() || '*' in raw || '#' in raw) return null
        return raw.takeIf { PhoneKey.digits(it).length >= 3 }
    }

    /**
     * Account của cuộc gọi hiện tại an toàn hơn fallback dùng cho lịch sử: nếu chỉ có một SIM đang
     * hoạt động thì đó chắc chắn là SIM của cuộc gọi mới, kể cả OEM dùng id tài khoản không chuẩn.
     */
    private fun currentSimCarrier(
        context: Context,
        phoneAccount: PhoneAccountHandle?,
    ): String? {
        if (phoneAccount != null) {
            val carrierFromAccount = runCatching {
                val base = context.getSystemService(TelephonyManager::class.java)
                val accountManager = base?.createForPhoneAccountHandle(phoneAccount)
                val operator = accountManager?.simOperator.orEmpty()
                SimInfo.normalizeCarrier(
                    name = accountManager?.simOperatorName,
                    mcc = operator.takeIf { it.length >= 5 }?.take(3),
                    mnc = operator.takeIf { it.length >= 5 }?.drop(3),
                )
            }.getOrNull()
            if (carrierFromAccount != null) return carrierFromAccount
        }

        val sims = runCatching { SimInfo.activeSims(context) }
            .onFailure { Log.w(LOG_TAG, "Unable to read active SIMs", it) }
            .getOrDefault(emptyList())

        val id = phoneAccount?.id?.trim()?.takeIf(String::isNotEmpty)
        if (id != null) {
            SimInfo.byAccountId(sims)[id]?.carrier?.let { return it }
            sims.firstOrNull { sim ->
                id.split(':', '/', ';', ',').any { token -> token == sim.subscriptionId.toString() }
            }?.carrier?.let { return it }
        }
        return sims.singleOrNull()?.carrier
    }
}
