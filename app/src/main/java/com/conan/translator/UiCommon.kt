package com.conan.translator

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

// UI shared by the bubble and the popup: dp and accent color in one place.
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.isNight(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

// accent blue; lighter version in the dark theme
fun Context.accentColor(): Int = if (isNight()) Color.rgb(0x8A, 0xB4, 0xF8) else Color.rgb(0x1A, 0x73, 0xE8)
