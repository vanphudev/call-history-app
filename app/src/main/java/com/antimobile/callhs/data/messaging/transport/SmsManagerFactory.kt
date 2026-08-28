package com.antimobile.callhs.data.messaging.transport

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

object SmsManagerFactory {
    @Suppress("DEPRECATION")
    fun forSubscription(context: Context, subscriptionId: Int): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        }
}

