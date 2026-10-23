package com.conan.translator

import android.content.Intent

// entry point: "Traduzir pt-BR" item in the selection menu. Only reads, never returns text.
class ProcessTextActivity : PopupActivity() {
    override fun textFrom(intent: Intent): String? =
        intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
}
