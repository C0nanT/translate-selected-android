package com.conan.tradutor

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

// UI comum da bolha e do popup: dp e cor de destaque num lugar so.
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.noturno(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

// azul de destaque; no tema escuro, versao mais clara
fun Context.corDestaque(): Int = if (noturno()) Color.rgb(0x8A, 0xB4, 0xF8) else Color.rgb(0x1A, 0x73, 0xE8)
