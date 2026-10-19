package com.conan.tradutor

// limits of the translation rules (checked by the cases in casos/); the other settings live in app/.../Config.kt
const val MAX_CHARS = 3000
// cap on the already-encoded URL (GET). CONSERVATIVE value, still NOT measured on the device:
// test on the tablet with ~3000 accented characters and note the real Google limit here.
const val MAX_URL = 8000
// target language (tl= in the URL). the "Traduzir pt-BR" label in the manifest must follow it
const val TARGET_LANGUAGE = "pt"
