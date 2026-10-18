package com.conan.tradutor

import android.content.ClipboardManager
import android.content.Intent

// entrada: toque na bolha traduz o que foi copiado.
// Android 10+ so libera a area de transferencia com a janela em foco.
class ClipboardActivity : PopupActivity() {
    override val lerComFoco = true
    override val avisaVazio = true

    override fun textoDe(intent: Intent): String? {
        val clip = try {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        } catch (e: SecurityException) { null }  // leitura bloqueada = nada copiado
        if (clip == null || clip.itemCount == 0) return null
        // so o texto do item; imagem/arquivo/URI conta como nada copiado (sem coerceToText)
        return clip.getItemAt(0).text?.toString()
    }
}
