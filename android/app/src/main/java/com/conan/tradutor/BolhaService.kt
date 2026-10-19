package com.conan.tradutor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.provider.Settings
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.service.quicksettings.TileService
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

// bolha flutuante na borda da tela: copiar no leitor + tocar aqui = traduzir.
// precisa de "Sobrepor a outros apps"; roda em primeiro plano (notificacao fixa).
class BolhaService : Service() {

    private lateinit var wm: WindowManager
    private var bolha: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACAO_DESLIGAR) { stopSelf(); return START_NOT_STICKY }
        // sem "Sobrepor a outros apps" a janela não pode ser adicionada: para em silêncio, sem erro nem pedido de reinício
        emPrimeiroPlano() // startForegroundService exige startForeground antes de parar, senão ForegroundServiceDidNotStartInTimeException
        if (!Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
            else @Suppress("DEPRECATION") stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        if (bolha == null) mostrarBolha()
        return START_STICKY
    }

    private fun emPrimeiroPlano() {
        val desligar = PendingIntent.getService(this, 0,
            Intent(this, BolhaService::class.java).setAction(ACAO_DESLIGAR), PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CANAL, "Bolha", NotificationManager.IMPORTANCE_MIN))
            Notification.Builder(this, CANAL)
        } else @Suppress("DEPRECATION") Notification.Builder(this)
        val n = b.setSmallIcon(R.drawable.ic_traduzir)
            .setContentTitle("Bolha do Tradutor ligada")
            .setContentText("Toque para desligar")
            .setContentIntent(desligar)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_traduzir), "Desligar", desligar).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun mostrarBolha() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val lado = dp(48)
        val tipo = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(lado, lado, tipo,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            val tela = resources.displayMetrics
            val salva = getSharedPreferences(PREFS, MODE_PRIVATE)
            val p = naTela(salva.getInt("x", tela.widthPixels - lado), salva.getInt("y", tela.heightPixels / 3), lado)
            x = p.x
            y = p.y
        }
        val v = TextView(this).apply {
            text = "文"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(corDestaque()) }
            alpha = 0.85f
        }
        // arrastar move (alem do slop do sistema); toque curto traduz; segurar parado desliga com vibracao
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val desligarSegurando = Runnable {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            stopSelf()
        }
        var x0 = 0; var y0 = 0; var rx = 0f; var ry = 0f; var arrastou = false
        v.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    x0 = lp.x; y0 = lp.y; rx = ev.rawX; ry = ev.rawY; arrastou = false
                    v.postDelayed(desligarSegurando, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - rx; val dy = ev.rawY - ry
                    if (!arrastou && (abs(dx) > slop || abs(dy) > slop)) {
                        arrastou = true
                        v.removeCallbacks(desligarSegurando)
                    }
                    if (arrastou) {
                        val p = naTela(x0 + dx.toInt(), y0 + dy.toInt(), lado)
                        lp.x = p.x; lp.y = p.y
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(desligarSegurando)
                    if (arrastou) {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    } else traduzirCopiado()
                }
                MotionEvent.ACTION_CANCEL -> v.removeCallbacks(desligarSegurando)
            }
            true
        }
        wm.addView(v, lp)
        bolha = v
        mudouEstado(true)
    }

    // bloco das Configuracoes rapidas acompanha o estado
    private fun mudouEstado(l: Boolean) {
        ligada = l
        if (Build.VERSION.SDK_INT >= 24) TileService.requestListeningState(this, ComponentName(this, BolhaTile::class.java))
    }

    // posicao da bolha (lado x lado) sem sair da tela
    private fun naTela(x: Int, y: Int, lado: Int): Position {
        val tela = resources.displayMetrics
        return clampPosition(Position(x, y), Size(tela.widthPixels, tela.heightPixels), Size(lado, lado), centeredX = false)
    }

    private val toques = Toques(TOQUE_REPETIDO_MS)

    private fun traduzirCopiado() {
        if (!toques.aceitar(SystemClock.uptimeMillis())) return  // toque repetido: um popup so
        startActivity(Intent(this, ClipboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onDestroy() {
        bolha?.let { wm.removeView(it) }
        bolha = null
        mudouEstado(false)
        super.onDestroy()
    }

    companion object {
        // bolha na tela agora (o processo morre junto com o servico, entao comeca falso)
        var ligada = false
            private set

        // sem "Sobrepor a outros apps" o servico para sozinho: quem chama confere antes
        fun ligar(ctx: Context) {
            val i = Intent(ctx, BolhaService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun desligar(ctx: Context) {
            ctx.stopService(Intent(ctx, BolhaService::class.java))
        }

        const val ACAO_DESLIGAR = "com.conan.tradutor.DESLIGAR"
        private const val CANAL = "bolha"
        private const val PREFS = "bolha"  // ultima posicao (x, y do canto superior esquerdo)
    }
}
