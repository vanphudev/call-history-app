package com.antimobile.mcas.data.messaging.transport

object SmsCallbackContract {
    const val ACTION_SENT = "com.antimobile.mcas.action.SMS_SENT"
    const val ACTION_DELIVERED = "com.antimobile.mcas.action.SMS_DELIVERED"
    const val EXTRA_ATTEMPT_ID = "attempt_id"
    const val EXTRA_PART_INDEX = "part_index"
    const val EXTRA_PROVIDER_ID = "provider_id"
}
