package com.antimobile.callhs.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
