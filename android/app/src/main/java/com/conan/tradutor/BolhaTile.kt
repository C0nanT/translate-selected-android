package com.conan.tradutor

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// bloco "Tradutor" nas Configuracoes rapidas: um toque liga/desliga a bolha.
@TargetApi(24)
class BolhaTile : TileService() {

    override fun onStartListening() = atualizar()

    override fun onClick() {
        val ligar = !BolhaService.ligada
        when {
            !ligar -> BolhaService.desligar(this)
            Settings.canDrawOverlays(this) -> BolhaService.ligar(this)
            // sem permissao: MainActivity leva para a tela de "Sobrepor a outros apps"
            else -> { abrirApp(); return }
        }
        // com o painel aberto o bloco nao recebe onStartListening de novo: mostra ja o estado pedido
        mostrar(ligar)
    }

    private fun atualizar() = mostrar(BolhaService.ligada)

    private fun mostrar(ligada: Boolean) {
        qsTile?.apply { state = if (ligada) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; updateTile() }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // versao com Intent so roda abaixo do 34
    private fun abrirApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Android 14+ so aceita PendingIntent aqui
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        else @Suppress("DEPRECATION") startActivityAndCollapse(i)
    }
}
