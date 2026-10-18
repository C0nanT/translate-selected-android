package com.conan.tradutor

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
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
import java.lang.ref.WeakReference

// popup ancorado embaixo, por cima do app de origem. Toque fora fecha (tema).
// some sozinho em HIDE_AFTER_S; dedo encostado pausa o tempo. Sem botoes.
// erro: fundo vermelho com o motivo (igual ao Linux).
// cada entrada (selecao, compartilhar...) so diz de onde vem o texto.
abstract class PopupActivity : Activity() {

    protected abstract fun textoDe(intent: Intent): String?

    private lateinit var label: TextView
    private lateinit var barra: BarraTempo
    private val painel = GradientDrawable()
    private val handler = Handler(Looper.getMainLooper())
    private var restanteMs = HIDE_MS
    private var ultimoTick = 0L
    private var pausado = false

    // so roda com o tempo correndo; pausa tira o tick da fila
    private val tick = object : Runnable {
        override fun run() {
            descontar()
            if (restanteMs <= 0) { finish(); return }
            barra.fracao = restanteMs.toFloat() / HIDE_MS
            handler.postDelayed(this, TICK_MS)
        }
    }

    private fun descontar() {
        val agora = SystemClock.uptimeMillis()
        restanteMs -= agora - ultimoTick
        ultimoTick = agora
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
            addView(ScrollLimitado(this@PopupActivity).apply { addView(label) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        }
        setContentView(raiz)

        // fundo proprio (cantos arredondados) p/ poder ficar vermelho no erro
        painel.cornerRadius = dp(12).toFloat()
        window.setBackgroundDrawable(InsetDrawable(painel, dp(8)))
        window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        ajustarLargura()

        mostrar("Traduzindo…")
        // rede fora da thread principal. a thread so guarda referencia fraca:
        // popup fechado nao fica preso na memoria e o resultado e descartado.
        val ref = WeakReference(this)
        Thread {
            val r = traduzir(texto, ::buscarHttp)
            Handler(Looper.getMainLooper()).post { ref.get()?.aplicar(r) }
        }.start()
    }

    private fun aplicar(r: Resultado) {
        if (isFinishing || isDestroyed) return
        when (r) {
            is Resultado.Traducao -> mostrar(r.texto)
            is Resultado.GrandeDemais -> mostrar("Texto grande demais (${r.n} caracteres)", erro = true)
            is Resultado.Erro -> mostrar(r.motivo, erro = true)
        }
    }

    // largura: ~POPUP_CHARS caracteres, sem passar da tela (refeito ao girar)
    private fun ajustarLargura() {
        val tela = resources.displayMetrics.widthPixels
        val desejada = label.paint.measureText("x".repeat(POPUP_CHARS)).toInt() + 2 * dp(16) + 2 * dp(8)
        window.setLayout(minOf(desejada, tela), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // configChanges no manifest: girar/redimensionar nao recria nem refaz o pedido
    override fun onConfigurationChanged(novo: Configuration) {
        super.onConfigurationChanged(novo)
        ajustarLargura()
    }

    // define o conteudo e reinicia o tempo. erro = fundo vermelho, texto branco (nos dois temas).
    fun mostrar(texto: String, erro: Boolean = false) {
        label.text = texto
        painel.setColor(if (erro) corErro() else corFundo())
        label.setTextColor(if (erro) Color.WHITE else corTexto())
        barra.cor = if (erro) Color.WHITE else corDestaque()
        reiniciarTempo()
    }

    private fun reiniciarTempo() {
        restanteMs = HIDE_MS
        ultimoTick = SystemClock.uptimeMillis()
        barra.fracao = 1f
        handler.removeCallbacks(tick)
        if (!pausado) handler.postDelayed(tick, TICK_MS)
    }

    // dedo no popup para o tick; soltar retoma de onde parou
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (!pausado) {
                pausado = true
                descontar()
                handler.removeCallbacks(tick)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (pausado) {
                pausado = false
                ultimoTick = SystemClock.uptimeMillis()
                handler.postDelayed(tick, TICK_MS)
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
    private fun corFundo() = if (noturno()) Color.rgb(0x2D, 0x2D, 0x2D) else Color.WHITE
    private fun corErro() = if (noturno()) Color.rgb(0x8C, 0x1D, 0x18) else Color.rgb(0xB3, 0x26, 0x1E)
    private fun corDestaque() = if (noturno()) Color.rgb(0x8A, 0xB4, 0xF8) else Color.rgb(0x1A, 0x73, 0xE8)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // barra fina que encolhe da esquerda p/ direita (ancorada a direita)
    private class BarraTempo(ctx: Activity, cor: Int) : View(ctx) {
        private val tinta = Paint().apply { color = cor }
        var cor: Int
            get() = tinta.color
            set(v) { if (v != tinta.color) { tinta.color = v; invalidate() } }
        var fracao = 1f
            set(v) { if (v != field) { field = v; invalidate() } }
        override fun onDraw(c: Canvas) {
            c.drawRect(width * (1 - fracao), 0f, width.toFloat(), height.toFloat(), tinta)
        }
    }

    // rolagem ate metade da altura da tela; acima disso o texto rola
    private class ScrollLimitado(ctx: Activity) : ScrollView(ctx) {
        override fun onMeasure(larg: Int, alt: Int) {
            val teto = resources.displayMetrics.heightPixels / 2
            val livre = MeasureSpec.getSize(alt)
            val max = if (MeasureSpec.getMode(alt) == MeasureSpec.UNSPECIFIED) teto else minOf(livre, teto)
            super.onMeasure(larg, MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST))
        }
    }

    private companion object {
        const val TICK_MS = 50L
        const val HIDE_MS = HIDE_AFTER_S * 1000L  // unico lugar que converte p/ ms
    }
}
