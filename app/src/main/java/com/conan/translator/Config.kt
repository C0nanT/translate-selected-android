package com.conan.translator

// app settings, as at the top of pc/popup.py.
// MAX_CHARS, MAX_URL and TARGET_LANGUAGE live in the core: core/.../Limits.kt
const val HIDE_AFTER_S = 15      // popup hides by itself (pauses with a finger on it)
const val POPUP_CHARS = 80       // maximum popup width, in characters
const val REPEATED_TAP_MS = 500L  // bubble taps closer together than this are ignored
