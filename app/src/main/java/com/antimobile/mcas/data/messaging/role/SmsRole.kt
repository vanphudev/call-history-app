package com.antimobile.mcas.data.messaging.role

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Nguồn duy nhất kiểm tra khả năng/ROLE_SMS và các quyền nhắn tin của MCAS. */
object SmsRole {

    private val coreRuntimePermissions: Array<String> = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
    )

    private val optionalMmsRuntimePermissions: Array<String> = arrayOf(
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.RECEIVE_WAP_PUSH,
    )

    /** Toàn bộ quyền do ROLE_SMS chuẩn AOSP quản lý; dùng để chẩn đoán khả năng SMS + MMS đầy đủ. */
    val runtimePermissions: Array<String> = coreRuntimePermissions + optionalMmsRuntimePermissions

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

    /** Chỉ các quyền bắt buộc để SMS văn bản hoạt động; MMS/WAP không được phép khóa onboarding. */
    fun missingCorePermissions(context: Context): List<String> = coreRuntimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    /** Quyền bổ sung để nhận MMS; một số ROM sideload không cấp dù ROLE_SMS đã được giữ. */
    fun missingOptionalMmsPermissions(context: Context): List<String> = optionalMmsRuntimePermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    /** Đủ toàn bộ quyền SMS/MMS mà manifest của ứng dụng mặc định đã khai báo. Không dùng để chặn UI. */
    fun hasRuntimePermissions(context: Context): Boolean = missingPermissions(context).isEmpty()

    fun hasCorePermissions(context: Context): Boolean = missingCorePermissions(context).isEmpty()

    fun hasOptionalMmsPermissions(context: Context): Boolean = missingOptionalMmsPermissions(context).isEmpty()

    fun canRead(context: Context): Boolean = isHeld(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun canSend(context: Context): Boolean = isHeld(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    private fun roleManager(context: Context): RoleManager? = context.getSystemService(RoleManager::class.java)

    private const val FEATURE_TELEPHONY_MESSAGING = "android.hardware.telephony.messaging"
}
