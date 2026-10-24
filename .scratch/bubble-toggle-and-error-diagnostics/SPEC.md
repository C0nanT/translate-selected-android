# Bubble toggle, popup entry-point contract, and error diagnostics

Status: ready-for-agent

Source: tech debt map for `app`, rows 1–3 (`.scratch/tech-debt-map/app/2026-10-23.md`), decisions recorded 2026-10-23.

## Problem Statement

Three problems in the Android app, from the user's and maintainer's side:

1. **Bubble without its off-switch notification.** On Android 13+, a user who turns the bubble on only from the Quick Settings tile is never asked for notification permission. The "Toque para desligar" notification, one of the documented ways to turn the bubble off, never appears. The app icon asks for the permission, the tile doesn't, because the on/off decision is written separately in both places and the copies have drifted.
2. **Re-translation on rotation is guarded by a comment.** A popup must translate once per open. That holds only because each popup entry point's manifest declaration carries a long `configChanges` list, copied by hand. A missing token (for example `uiMode`) builds fine, but rotating the device or switching light/dark sends the text to Google again and restarts the auto-hide timer. The next entry point (for example share) is likely to copy it wrong.
3. **Failures leave no trace.** Network failures the app doesn't classify (TLS handshake errors, connection resets) reach the user as raw English exception text instead of the pt-BR "no connection" message. Nothing is ever written to logcat. A clipboard read that Android blocks looks exactly like an empty clipboard, so "Nada copiado" bug reports can't be diagnosed.

## Solution

1. One shared, pure on/off decision used by both the app icon and the Quick Settings tile. On Android 13+, the first time the tile turns the bubble on without notification permission, it routes through the app, which asks for the permission and turns the bubble on. After that, the tile turns the bubble on directly and doesn't reopen the app each time.
2. A unit test that reads the app manifest and fails the build when any popup entry point is missing part of the required contract.
3. TLS and socket-level failures are shown as "Sem conexão com a internet". Every converted or swallowed exception on these paths is logged with its stack trace. The user-visible messages for every other case stay exactly as today.

## User Stories

1. As a user on Android 13+, I want the "Toque para desligar" notification to appear even when I turn the bubble on from the Quick Settings tile, so that I always have a way to turn it off from the notification shade.
2. As a user turning the bubble on from the tile for the first time on Android 13+, I want to be asked for notification permission once, so that I can choose whether to allow it.
3. As a user who denied the notification permission, I want later tile taps to turn the bubble on straight away, so that the panel doesn't close and reopen the app every time.
4. As a user, I want denying the notification permission to still leave the bubble on, so that the permission stays optional, as it is from the app icon today.
5. As a user without "Sobrepor a outros apps", I want both the tile and the app icon to send me to that settings screen, so that I can grant it, as today.
6. As a user with the bubble on, I want tapping the tile or the app icon to turn it off, as today.
7. As a user on Android 12 or older, I want the tile and the app icon to behave exactly as they do today, since there's no runtime notification permission there.
8. As a user turning the bubble on from the app icon, I want the toasts ("Bolha ligada…", "Bolha desligada", the overlay hint) to stay as they are.
9. As a user turning the bubble on from the tile, I want the tile to keep showing the requested state right away, as today.
10. As a maintainer adding a precondition for turning the bubble on, I want to change one decision, so that the tile and the app icon can't drift apart again.
11. As a maintainer, I want the on/off decision covered by JVM unit tests, so that I can change it without a device.
12. As a user, I want rotating the device while a popup is open to keep the same translation and remaining time, without a new request.
13. As a user, I want switching light/dark mode while a popup is open to keep the same translation and remaining time, without a new request.
14. As a maintainer adding a popup entry point, I want the unit tests to fail when its manifest declaration lacks the popup theme, any required `configChanges` token, or `excludeFromRecents`, so that the mistake is caught before it ships.
15. As a maintainer, I want that test's failure message to name the activity and the missing piece, so that the fix is obvious.
16. As a user on a flaky network, I want a TLS handshake failure to show "Sem conexão com a internet" rather than an English Java message.
17. As a user on a flaky network, I want a connection reset to show "Sem conexão com a internet".
18. As a user, I want timeouts, HTTP errors, and every other failure to keep showing exactly the message they show today (including the raw detail for unclassified errors, kept on purpose to match the PC version).
19. As a maintainer reading a bug report, I want every network failure logged to logcat with its stack trace, so that I can see what actually happened.
20. As a maintainer, I want a clipboard read that Android blocked to be logged, so that I can tell it apart from an empty clipboard when someone reports "Nada copiado".
21. As a user, I want a blocked clipboard read to keep showing "Nada copiado" as today.
22. As a user, I want closing a popup mid-request to keep behaving as today (the result is discarded, and nothing extra appears).

## Implementation Decisions

- **Toggle decision (new, pure, in `app`).** A small Android-free function in the style of `Taps`. Inputs: whether the bubble is on now, whether the overlay permission is granted, whether the runtime notification permission applies (API 33+), whether it is granted, and whether the app has already asked for it once. Output, one of:
  - **Disable**
  - **AskOverlay**: open the overlay settings screen. Only the app icon can do this directly; the tile reaches it by opening the app, as today.
  - **Enable**
  - **EnableViaApp**: open the app so it can request the permission and enable.

  The tile only ever acts on Disable / Enable / EnableViaApp-or-AskOverlay (both open the app). The app icon never gets EnableViaApp, because it is the app: on Android 13+ without the permission, it requests it and enables.
- **"Asked once" flag.** Stored in the app's SharedPreferences and set when the app actually issues the notification permission request. The tile reads it to choose between Enable and EnableViaApp. Approved decision (2026-10-23): ask only the first time.
- **App icon behaviour unchanged.** It still requests the permission on every enable while it is not granted, as today. After two denials Android stops showing the dialog, and the activity just finishes. Only the tile consults the "asked once" flag.
- **Tile.** It keeps its optimistic tile-state update for Disable and Enable. For EnableViaApp it opens the app through the same path it already uses for the missing-overlay case. The tile cannot request permissions itself.
- **Dead check removed.** The `SDK >= 23` guard on the overlay check in the app icon path is always true (minSdk 23). It goes away as part of moving to the shared decision.
- **Manifest contract.** The required set is: theme `@style/Popup`; `configChanges` containing all of `orientation`, `screenSize`, `smallestScreenSize`, `screenLayout`, `keyboard`, `keyboardHidden`, `navigation`, `uiMode`; `excludeFromRecents="true"`. "Popup entry point" means any `<activity>` whose theme is `@style/Popup`. The `PopupActivity` comment that lists the contract stays and points at the test.
- **Exception classification.** `convertException` additionally maps `javax.net.ssl.SSLException` (which covers handshake errors) and `java.net.SocketException` (which covers connection reset) to `NoConnection`. Existing mappings keep precedence: `ConnectException` and `NoRouteToHostException` are `SocketException` subclasses and already map to `NoConnection`. `NetworkError` passes through unchanged. Everything else stays `Other(detail)`. Approved decision (2026-10-23): keep the raw detail for unclassified errors.
- **Logging.** `Log.w` with the throwable at the `HttpFetcher.fetch` catch (before conversion), and at the clipboard `SecurityException` catch. One shared tag for the app. `convertException` itself stays pure and does not log, so it remains JVM-testable without Android stubs.
- **No change to core.** `NetworkError`, its messages and `cases/*.json` are untouched.

## Testing Decisions

- Good tests here assert outcomes through a public seam: which action the decision returns, which manifest entries break the contract, which `NetworkError` an exception becomes. They don't assert how the code gets there, and they don't touch Android framework classes.
- **Seam A: toggle decision.** A JVM test over the pure function, covering every input combination that matters: on → Disable; off without overlay → AskOverlay; off with overlay below API 33 → Enable; API 33+ with permission granted → Enable; API 33+ without permission, never asked → EnableViaApp; API 33+ without permission, already asked → Enable.
- **Seam B: manifest contract.** A JVM test that parses the app's `AndroidManifest.xml` from the source tree (the way core tests read the shared `cases/` files from the repo root) and asserts that every `@style/Popup` activity meets the contract. It includes a negative check, against a small inline manifest string, that a missing `uiMode` is reported with the activity name.
- **Seam C: `convertException`** (existing). New cases go in the existing network tests: an `SSLHandshakeException` and a `SocketException("Connection reset")` map to `NoConnection`, and the existing cases still pass unchanged.
- **Not tested automatically.** The `Log.w` calls (side effects only), and the tile's open-app routing (Android framework). Check them manually on an API 33+ device: fresh install, grant overlay, enable from the tile, confirm the permission prompt and the notification; deny it, toggle from the tile again, confirm there is no second prompt.
- **Modules tested:** `app` only (JUnit 4, `app/src/test`).
- **Prior art:** the `Taps` tests (pure decision with time injected), the existing network tests for `convertException`, and core's tests reading `cases/*.json` from the repo root.

## Out of Scope

- Saving popup state across recreation (`onSaveInstanceState`) so the once-per-open rule holds even with a wrong manifest. Decided 2026-10-23: the manifest test is enough, and this would need Robolectric.
- Any other row of the tech debt map: stuck "Traduzindo…" (5), the `addView` guard (6), the fetcher seam (7), the gesture classifier (8), message ownership between app and core (9), R8 (10), and the repeated-tap popup rule (4).
- Changing user-facing message text, or moving messages into core.
- Replacing the `client=gtx` endpoint (recorded as intentional).
- Replacing the process-global "bubble enabled" flag.

## Further Notes

- Mapping every `SSLException` to "Sem conexão" also covers certificate failures (for example a captive portal or an intercepting proxy). That is acceptable for a sideload app: from the user's side the network is effectively not usable. The logged stack trace keeps the real cause visible.
- `cancel()` disconnecting a connection mid-read can surface as a `SocketException`, which will now become `NoConnection`. That changes nothing visible, because the popup is closing and the result is discarded.
- The repo's SOLID section (`AGENTS.md`) applies: the toggle decision is policy kept Android-free and passed its inputs, while the tile, the app icon and the `Log` calls are details.
