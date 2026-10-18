package com.conan.tradutor

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// popup ancorado embaixo, por cima do app de origem. Toque fora fecha (tema).
// some sozinho em HIDE_AFTER_S; dedo encostado pausa o tempo. Sem botoes.
// cada entrada (selecao, compartilhar...) so diz de onde vem o texto.
abstract class PopupActivity : Activity() {

    protected abstract fun textoDe(intent: Intent): String?

    private lateinit var label: TextView
    private lateinit var barra: BarraTempo
    private val handler = Handler(Looper.getMainLooper())
    private var restanteMs = HIDE_AFTER_S * 1000L
    private var ultimoTick = 0L
    private var pausado = false

    private val tick = object : Runnable {
        override fun run() {
            val agora = SystemClock.uptimeMillis()
            if (!pausado) restanteMs -= agora - ultimoTick
            ultimoTick = agora
            if (restanteMs <= 0) { finish(); return }
            barra.fracao = restanteMs / (HIDE_AFTER_S * 1000f)
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val texto = textoDe(intent)?.trim().orEmpty()
        if (texto.isEmpty()) { finish(); return }

        val pad = dp(16)
        label = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(pad, pad, pad, pad)
        }
        barra = BarraTempo(this, corDestaque())
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(ScrollView(this@PopupActivity).apply { addView(label) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        }
        setContentView(raiz)

        // largura: ~POPUP_CHARS caracteres, sem passar da tela
        val tela = resources.displayMetrics.widthPixels
        val desejada = label.paint.measureText("x".repeat(POPUP_CHARS)).toInt() + 2 * pad
        window.setLayout(minOf(desejada, tela - dp(16)), ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)

        mostrar("Traduzindo…")
        // rede fora da thread principal; resultado apos fechar o popup e descartado
        Thread {
            val r = traduzir(texto, ::buscarHttp)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                when (r) {
                    is Resultado.Traducao -> mostrar(r.texto)
                    is Resultado.GrandeDemais -> mostrar("Texto grande demais (${r.n} caracteres)", erro = true)
                    is Resultado.Erro -> mostrar(r.motivo, erro = true)
                }
            }
        }.start()
    }

    // define o conteudo e reinicia o tempo. erro = vermelho legivel nos dois temas.
    fun mostrar(texto: String, erro: Boolean = false) {
        label.text = texto
        label.setTextColor(
            if (erro) (if (noturno()) Color.rgb(0xF2, 0x8B, 0x82) else Color.rgb(0xB3, 0x26, 0x1E))
            else corTexto()
        )
        barra.cor = if (erro) label.currentTextColor else corDestaque()
        reiniciarTempo()
    }

    private fun reiniciarTempo() {
        restanteMs = HIDE_AFTER_S * 1000L
        ultimoTick = SystemClock.uptimeMillis()
        barra.fracao = 1f
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
    }

    // dedo no popup pausa; soltar retoma de onde parou
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> pausado = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pausado = false
                ultimoTick = SystemClock.uptimeMillis()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun noturno() =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private fun corTexto() = if (noturno()) Color.rgb(0xE6, 0xE6, 0xE6) else Color.rgb(0x20, 0x20, 0x20)
    private fun corDestaque() = if (noturno()) Color.rgb(0x8A, 0xB4, 0xF8) else Color.rgb(0x1A, 0x73, 0xE8)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // barra fina que encolhe da esquerda p/ direita (ancorada a direita)
    private class BarraTempo(ctx: Activity, cor: Int) : View(ctx) {
        private val tinta = Paint().apply { color = cor }
        var cor: Int
            get() = tinta.color
            set(v) { tinta.color = v; invalidate() }
        var fracao = 1f
            set(v) { field = v; invalidate() }
        override fun onDraw(c: Canvas) {
            c.drawRect(width * (1 - fracao), 0f, width.toFloat(), height.toFloat(), tinta)
        }
    }

    private companion object { const val TICK_MS = 50L }
}
