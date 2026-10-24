package com.conan.translator

import android.content.ClipboardManager
import android.content.Intent
import android.util.Log

// entry point: tapping the bubble translates what was copied.
// Android 10+ only exposes the clipboard while the window has focus.
class ClipboardActivity : PopupActivity() {
    override val readWithFocus = true
    override val warnWhenEmpty = true

    override fun textFrom(intent: Intent): String? {
        val clip = try {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        } catch (e: SecurityException) {  // blocked read = nothing copied
            Log.w(LOG_TAG, "clipboard read blocked", e)
            null
        }
        if (clip == null || clip.itemCount == 0) return null
        // only the item's text; image/file/URI counts as nothing copied (no coerceToText)
        return clip.getItemAt(0).text?.toString()
    }
}
