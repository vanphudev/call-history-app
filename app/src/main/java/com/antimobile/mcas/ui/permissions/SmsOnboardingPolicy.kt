package com.antimobile.mcas.ui.permissions

/**
 * Phân biệt quyền tối thiểu để dùng SMS văn bản với quyền bổ sung dành cho MMS.
 *
 * Một số ROM không tự cấp hoặc không cho xin lại RECEIVE_MMS/RECEIVE_WAP_PUSH dù ứng dụng đang giữ
 * ROLE_SMS. Hai quyền đó không được phép giữ người dùng vô hạn ở onboarding khi các quyền SMS lõi đã đủ.
 */
internal data class SmsOnboardingPermissionState(
    val roleHeld: Boolean,
    val corePermissionsGranted: Boolean,
    val optionalMmsPermissionsGranted: Boolean,
) {
    /** Đủ để rời onboarding và sử dụng SMS văn bản. */
    val canCompleteOnboarding: Boolean
        get() = roleHeld && corePermissionsGranted

    /** Đủ toàn bộ quyền cho cả SMS và khả năng nhận MMS/WAP push. */
    val hasFullMessagingPermissions: Boolean
        get() = canCompleteOnboarding && optionalMmsPermissionsGranted
}

/** Thiết bị không hỗ trợ ROLE_SMS phải được bỏ qua để onboarding không thể kẹt. */
internal fun isSmsOnboardingComplete(
    shouldRequestSms: Boolean,
    permissionState: SmsOnboardingPermissionState,
): Boolean = !shouldRequestSms || permissionState.canCompleteOnboarding
