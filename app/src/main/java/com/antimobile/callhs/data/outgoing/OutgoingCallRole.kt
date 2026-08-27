package com.antimobile.callhs.data.outgoing

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/**
 * ROLE_CALL_REDIRECTION là callback chuyên biệt xảy ra trước khi Telecom đặt cuộc gọi đi và cung cấp
 * PhoneAccountHandle ban đầu. CallHS chỉ quan sát rồi luôn cho cuộc gọi đi nguyên trạng.
 */
object OutgoingCallRole {
    fun isAvailable(context: Context): Boolean = roleManager(context)
        ?.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) == true

    fun isHeld(context: Context): Boolean = roleManager(context)
        ?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true

    fun requestIntent(context: Context): Intent? = roleManager(context)
        ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) }
        ?.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)

    private fun roleManager(context: Context): RoleManager? =
        context.getSystemService(RoleManager::class.java)
}
