# 02: Tile asks for notification permission once

> **Difficulty:** Standard: **suggested model:** Sonnet (Claude Code) / Sonnet (Cursor). Suggestion only, use whatever model you have to hand.

**Spec:** ../SPEC.md

**What to build:** On Android 13+, when a user turns the bubble on from the Quick Settings tile without notification permission and the app has never asked for it, the tile opens the app (through the same path it uses for the missing-overlay case). The app asks for the permission and turns the bubble on, so the "Toque para desligar" notification appears once granted. The app records in SharedPreferences that it has asked, at the moment it actually issues the request. From then on, the tile turns the bubble on directly even if the permission was denied. The app icon keeps asking on every enable while the permission is not granted, as today. Android 12 and older are unchanged.

**Seams:**
- Seam A: toggle decision (pure function, JVM test). The "never asked → EnableViaApp" and "already asked → Enable" cases now drive real tile behaviour.

**Blocked by:** 01 (Shared bubble on/off decision)

Status: ready-for-agent

- [x] The tile passes the real "asked once" flag into the decision and opens the app on `EnableViaApp`
- [x] The app sets the "asked once" flag in prefs when it issues the POST_NOTIFICATIONS request
- [x] Denying the permission still leaves the bubble on
- [x] Seam A tests for the API 33+ never-asked and already-asked cases pass
- [ ] Manual check on an API 33+ device: fresh install, grant overlay, enable from the tile → prompt shown, notification visible after granting; reinstall, deny, toggle from the tile again → no second prompt, bubble turns on
- [x] `./gradlew :app:testDebugUnitTest` passes
