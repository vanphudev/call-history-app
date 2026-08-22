package com.antimobile.callhs.ui.blocking

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.callhs.data.blocking.CallBlockNotifier

/**
 * Trạng thái notification dùng chung cho hai destination blocking. Việc giữ launcher và cờ đã hỏi
 * ở cấp AppNav ngăn màn chính và màn cài đặt mở hai hộp xin quyền liên tiếp.
 */
@Stable
class CallBlockNotificationUiState internal constructor(
    private val appContext: Context,
    initialPermissionAskedThisSession: Boolean,
    private val persistPermissionAsked: () -> Unit,
) {
    var readiness by mutableStateOf(CallBlockNotifier.readiness(appContext))
        private set

    var permissionAskedThisSession by mutableStateOf(initialPermissionAskedThisSession)
        private set

    private var launchPermission: (() -> Unit)? = null

    internal fun bindPermissionLauncher(launcher: () -> Unit) {
        launchPermission = launcher
    }

    internal fun refresh() {
        readiness = CallBlockNotifier.readiness(appContext)
    }

    fun requestFirstPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            readiness == CallBlockNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED &&
            !permissionAskedThisSession
        ) {
            val launcher = launchPermission ?: return
            // Ghi state trước khi launch để hai destination đang overlap trong animation không thể
            // cùng mở hai permission dialog.
            permissionAskedThisSession = true
            persistPermissionAsked()
            launcher()
        }
    }

    fun repair() {
        when {
            readiness == CallBlockNotifier.Readiness.READY -> Unit
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                readiness == CallBlockNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED &&
                !permissionAskedThisSession -> requestFirstPermission()
            else -> CallBlockNotifier.openNotificationSettings(appContext)
        }
    }
}

@Composable
fun rememberCallBlockNotificationUiState(): CallBlockNotificationUiState {
    val appContext = LocalContext.current.applicationContext
    var savedPermissionAskedThisSession by rememberSaveable { mutableStateOf(false) }
    val state = remember(appContext) {
        CallBlockNotificationUiState(
            appContext = appContext,
            initialPermissionAskedThisSession = savedPermissionAskedThisSession,
            persistPermissionAsked = { savedPermissionAskedThisSession = true },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state.refresh()
    }
    // Gán đồng bộ trước khi NavHost dựng các destination con; LaunchedEffect auto-request ở màn con
    // không thể chạy với một launcher no-op trong cùng frame composition đầu tiên.
    state.bindPermissionLauncher {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.refresh()
    }

    return state
}
