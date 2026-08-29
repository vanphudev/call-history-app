package com.antimobile.mcas.data.blocking

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/** Cổng nhỏ cho ROLE_CALL_SCREENING để UI không tự kiểm tra role theo nhiều cách khác nhau. */
object CallScreeningRole {

    fun isAvailable(context: Context): Boolean = roleManager(context)
        ?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun isHeld(context: Context): Boolean = roleManager(context)
        ?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    /** Intent hệ thống cho người dùng chọn ứng dụng sàng lọc cuộc gọi mặc định. */
    fun requestIntent(context: Context): Intent? = roleManager(context)
        ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
        ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)

    private fun roleManager(context: Context): RoleManager? =
        context.getSystemService(RoleManager::class.java)
}
