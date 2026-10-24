package com.conan.translator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

// remembers that the app already issued the POST_NOTIFICATIONS request, so the tile stops sending users to the app
object NotificationAsk {
    private const val FILE = "notification_ask"
    private const val KEY = "asked"

    // POST_NOTIFICATIONS only exists (and needs granting) on Android 13+
    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun wasAsked(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markAsked(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}
