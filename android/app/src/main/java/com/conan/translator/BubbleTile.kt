package com.conan.translator

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// "Tradutor" tile in Quick Settings: one tap turns the bubble on/off.
@TargetApi(24)
class BubbleTile : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        val enable = !BubbleService.enabled
        when {
            !enable -> BubbleService.disable(this)
            Settings.canDrawOverlays(this) -> BubbleService.enable(this)
            // no permission: MainActivity leads to the "Sobrepor a outros apps" screen
            else -> { openApp(); return }
        }
        // with the panel open the tile does not get onStartListening again: show the requested state now
        show(enable)
    }

    private fun refresh() = show(BubbleService.enabled)

    private fun show(enabled: Boolean) {
        qsTile?.apply { state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; updateTile() }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated") // the Intent version only runs below 34
    private fun openApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Android 14+ only accepts a PendingIntent here
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        else @Suppress("DEPRECATION") startActivityAndCollapse(i)
    }
}
