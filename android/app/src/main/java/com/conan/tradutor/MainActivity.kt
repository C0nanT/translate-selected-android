package com.conan.tradutor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

// icone do app: liga a bolha. 1a vez abre a tela de "Sobrepor a outros apps".
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative \"Sobrepor a outros apps\" para o Tradutor e toque no ícone de novo", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            val i = Intent(this, BolhaService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            Toast.makeText(this, "Bolha ligada: copie o texto e toque na bolha", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
