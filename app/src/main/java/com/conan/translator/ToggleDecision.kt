package com.conan.translator

enum class ToggleAction { Disable, AskOverlay, Enable, EnableViaApp }

// what a bubble on/off request should do. No Android: every input comes in as a parameter.
// EnableViaApp = enable, but the app must first request the Android 13+ notification permission.
fun decideToggle(
    enabled: Boolean,
    canDrawOverlays: Boolean,
    sdk: Int,
    notificationGranted: Boolean,
    notificationAsked: Boolean,
): ToggleAction = when {
    enabled -> ToggleAction.Disable
    !canDrawOverlays -> ToggleAction.AskOverlay
    sdk >= 33 && !notificationGranted && !notificationAsked -> ToggleAction.EnableViaApp
    else -> ToggleAction.Enable
}
