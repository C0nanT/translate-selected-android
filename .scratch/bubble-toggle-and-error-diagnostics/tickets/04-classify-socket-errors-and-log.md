# 04: "No connection" for TLS/socket errors, plus logging

> **Difficulty:** Light: **suggested model:** Haiku (Claude Code) / claude-4.5-haiku-thinking (Cursor). Suggestion only, use whatever model you have to hand.

**Spec:** ../SPEC.md

**What to build:** On a flaky network, a TLS handshake failure or a connection reset shows "Sem conexão com a internet" instead of a raw English exception message. Every other failure keeps its message exactly as today, including the raw detail for unclassified errors. Every exception caught in the HTTP fetch is logged with `Log.w` and its stack trace before conversion. A clipboard read blocked by Android (`SecurityException`) is logged too, and still shows "Nada copiado". Use one shared log tag for the app. The exception-conversion function stays pure and does not log.

**Seams:**
- Seam C: `convertException` (existing network tests)

**Blocked by:** None (can start immediately)

Status: ready-for-human

- [x] Tests written first: `SSLHandshakeException` → `NoConnection`, `SocketException("Connection reset")` → `NoConnection`
- [x] Existing network tests pass unchanged (timeout, unknown host/refused/no route, pass-through, other with detail)
- [x] `Log.w(TAG, …, e)` in the fetch catch and in the clipboard `SecurityException` catch, with no change to the user-visible message
- [x] `convertException` has no `android.*` dependency
- [x] `./gradlew :app:testDebugUnitTest` passes
