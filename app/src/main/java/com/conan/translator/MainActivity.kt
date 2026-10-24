package com.conan.translator

import android.Manifest
import android.app.Activity
import android.content.Intent
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
        val action = decideToggle(
            enabled = BubbleService.enabled,
            canDrawOverlays = Settings.canDrawOverlays(this),
            sdk = Build.VERSION.SDK_INT,
            notificationGranted = NotificationAsk.isGranted(this),
            notificationAsked = false,  // on purpose: the app icon asks on every enable while not granted
        )
        when (action) {
            ToggleAction.Disable -> {
                BubbleService.disable(this)
                Toast.makeText(this, "Bolha desligada", Toast.LENGTH_SHORT).show()
            }
            ToggleAction.AskOverlay -> {
                Toast.makeText(this, "Ative \"Sobrepor a outros apps\" para o Tradutor e toque no ícone de novo", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            ToggleAction.Enable, ToggleAction.EnableViaApp -> {
                // Android 13+: asks for the "Toque para desligar" notification; denying it does not block the bubble
                if (action == ToggleAction.EnableViaApp) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                    NotificationAsk.markAsked(this)
                    awaitingPermission = true
                }
                BubbleService.enable(this)
                Toast.makeText(this, "Bolha ligada: copie o texto e toque na bolha", Toast.LENGTH_SHORT).show()
            }
        }
        if (!awaitingPermission) finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        finish()
    }
}
