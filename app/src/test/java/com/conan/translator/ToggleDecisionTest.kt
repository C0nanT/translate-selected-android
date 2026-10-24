package com.conan.translator

import org.junit.Assert.assertEquals
import org.junit.Test

class ToggleDecisionTest {
    private fun decide(
        enabled: Boolean = false,
        overlay: Boolean = true,
        sdk: Int = 33,
        notification: Boolean = false,
        asked: Boolean = false,
    ) = decideToggle(enabled, overlay, sdk, notification, asked)

    @Test fun onTurnsOff() {
        assertEquals(ToggleAction.Disable, decide(enabled = true))
    }

    @Test fun offWithoutOverlayAsksForOverlay() {
        assertEquals(ToggleAction.AskOverlay, decide(overlay = false))
    }

    @Test fun offWithOverlayBelowApi33Enables() {
        assertEquals(ToggleAction.Enable, decide(sdk = 32))
    }

    @Test fun api33WithNotificationGrantedEnables() {
        assertEquals(ToggleAction.Enable, decide(notification = true))
    }

    @Test fun api33WithoutNotificationNeverAskedEnablesViaApp() {
        assertEquals(ToggleAction.EnableViaApp, decide())
    }

    @Test fun api33WithoutNotificationAlreadyAskedEnables() {
        assertEquals(ToggleAction.Enable, decide(asked = true))
    }
}
