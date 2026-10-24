package com.conan.translator

import android.content.Context

// remembers that the app already issued the POST_NOTIFICATIONS request, so the tile stops sending users to the app
object NotificationAsk {
    private const val FILE = "notification_ask"
    private const val KEY = "asked"

    fun wasAsked(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun markAsked(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}
