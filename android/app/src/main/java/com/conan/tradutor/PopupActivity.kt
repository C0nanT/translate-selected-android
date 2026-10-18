package com.conan.tradutor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView

// popup ancorado embaixo, por cima do app de origem. Toque fora fecha (tema).
// cada entrada (selecao, compartilhar...) so diz de onde vem o texto.
abstract class PopupActivity : Activity() {

    protected abstract fun textoDe(intent: Intent): String?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val texto = textoDe(intent)?.trim().orEmpty()
        if (texto.isEmpty()) { finish(); return }

        val pad = dp(16)
        val label = TextView(this).apply {
            text = texto
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(pad, pad, pad, pad)
        }
        setContentView(ScrollView(this).apply { addView(label) })

        // largura: ~POPUP_CHARS caracteres, sem passar da tela
        val tela = resources.displayMetrics.widthPixels
        val desejada = label.paint.measureText("x".repeat(POPUP_CHARS)).toInt() + 2 * pad
        window.setLayout(minOf(desejada, tela - dp(16)), ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
