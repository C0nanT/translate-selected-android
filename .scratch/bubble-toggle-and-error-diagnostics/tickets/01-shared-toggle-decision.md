# 01: Shared bubble on/off decision (prefactor)

> **Difficulty:** Standard: **suggested model:** Sonnet (Claude Code) / Sonnet (Cursor). Suggestion only, use whatever model you have to hand.

**Spec:** ../SPEC.md

**What to build:** The app icon and the Quick Settings tile stop deciding on/off each in their own way, and both ask one pure, Android-free decision (in the style of `Taps`). Users see no change: the app icon still turns the bubble off, sends the user to "Sobrepor a outros apps", or requests the notification permission and enables, with the same toasts. The tile still turns the bubble off, opens the app when the overlay permission is missing, or enables directly, and keeps its optimistic tile-state update. The decision already has the inputs and the `EnableViaApp` outcome from the spec, but in this ticket the tile is wired so that it never produces `EnableViaApp` (for example, by passing "already asked" as true), so its behaviour stays as today. Remove the always-true `SDK >= 23` guard on the app icon's overlay check.

**Seams:**
- Seam A: toggle decision (pure function, JVM test)

**Blocked by:** None (can start immediately)

Status: ready-for-human

- [x] One pure decision function in `app` with no `android.*` imports, returning Disable / AskOverlay / Enable / EnableViaApp
- [x] JVM tests cover every combination listed in the spec's Seam A
- [x] The app icon and the tile both act on that decision, and no on/off branching is left in either caller
- [x] Toasts, the permission request from the app icon, the tile-state update and the open-app path are unchanged
- [x] The dead `SDK >= 23` check is gone
- [x] `./gradlew :app:testDebugUnitTest` passes
