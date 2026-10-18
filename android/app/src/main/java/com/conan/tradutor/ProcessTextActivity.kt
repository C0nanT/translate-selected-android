package com.conan.tradutor

import android.content.Intent

// entrada: item "Traduzir pt-BR" do menu da selecao. So le, nunca devolve texto.
class ProcessTextActivity : PopupActivity() {
    override fun textoDe(intent: Intent): String? =
        intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
}
