package com.antimobile.mcas.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Kiểm tra thiết bị có kết nối mạng đi Internet hay không (đã xác thực). Rẻ, gọi ở luồng chính OK. */
object NetworkUtil {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
