package com.antimobile.callhs.data.messaging.role

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Nguồn duy nhất kiểm tra khả năng/ROLE_SMS và các quyền nhắn tin của CallHS. */
object SmsRole {

    private val coreRuntimePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE,
    )

    val runtimePermissions: Array<String> = coreRuntimePermissions + arrayOf(
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_WAP_PUSH,
    )

    fun isMessagingSupported(context: Context): Boolean {
        val pm = context.packageManager
        // Một số máy Android 10–12 có modem/SMS và ROLE_SMS nhưng chưa quảng bá feature
        // TELEPHONY_MESSAGING mới. ROLE availability ở bước kế tiếp vẫn là chốt xác nhận cuối.
        return pm.hasSystemFeature(FEATURE_TELEPHONY_MESSAGING) ||
            pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    fun isAvailable(context: Context): Boolean = roleManager(context)
        ?.isRoleAvailable(RoleManager.ROLE_SMS) == true

    fun isHeld(context: Context): Boolean = roleManager(context)
        ?.isRoleHeld(RoleManager.ROLE_SMS) == true

    fun requestIntent(context: Context): Intent? = roleManager(context)
        ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_SMS) }
        ?.createRequestRoleIntent(RoleManager.ROLE_SMS)

    fun missingPermissions(context: Context): List<String> = runtimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    fun hasCorePermissions(context: Context): Boolean = coreRuntimePermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun canRead(context: Context): Boolean = isHeld(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun canSend(context: Context): Boolean = isHeld(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    private fun roleManager(context: Context): RoleManager? = context.getSystemService(RoleManager::class.java)

    private const val FEATURE_TELEPHONY_MESSAGING = "android.hardware.telephony.messaging"
}
