package com.conan.translator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

// app icon: turns the bubble on/off. The first time it opens the "Sobrepor a outros apps" screen.
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var awaitingPermission = false
        if (BubbleService.enabled) {
            BubbleService.disable(this)
            Toast.makeText(this, "Bolha desligada", Toast.LENGTH_SHORT).show()
        } else if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative \"Sobrepor a outros apps\" para o Tradutor e toque no ícone de novo", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            // Android 13+: asks for the "Toque para desligar" notification; denying it does not block the bubble
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                awaitingPermission = true
            }
            BubbleService.enable(this)
            Toast.makeText(this, "Bolha ligada: copie o texto e toque na bolha", Toast.LENGTH_SHORT).show()
        }
        if (!awaitingPermission) finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        finish()
    }
}
