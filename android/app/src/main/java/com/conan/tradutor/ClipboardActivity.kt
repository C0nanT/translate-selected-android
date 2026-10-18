package com.conan.tradutor

import android.content.ClipboardManager
import android.content.Intent

// entrada: toque na bolha traduz o que foi copiado.
// Android 10+ so libera a area de transferencia com a janela em foco.
class ClipboardActivity : PopupActivity() {
    override val lerComFoco = true

    override fun textoDe(intent: Intent): String? {
        val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        if (clip == null || clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()
    }
}
