package com.antimobile.callhs.data.messaging.transport

object SmsCallbackContract {
    const val ACTION_SENT = "com.antimobile.callhs.action.SMS_SENT"
    const val ACTION_DELIVERED = "com.antimobile.callhs.action.SMS_DELIVERED"
    const val EXTRA_ATTEMPT_ID = "attempt_id"
    const val EXTRA_PART_INDEX = "part_index"
    const val EXTRA_PROVIDER_ID = "provider_id"
}

