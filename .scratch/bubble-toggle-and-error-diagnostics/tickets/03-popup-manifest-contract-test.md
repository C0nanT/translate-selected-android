# 03: Unit test for the popup entry-point contract

> **Difficulty:** Standard: **suggested model:** Sonnet (Claude Code) / Sonnet (Cursor). Suggestion only, use whatever model you have to hand.

**Spec:** ../SPEC.md

**What to build:** Breaking the "translate once per open" contract in the manifest fails the unit tests instead of shipping. A JVM test reads the app's real `AndroidManifest.xml` from the source tree. For every `<activity>` whose theme is `@style/Popup`, it asserts that `configChanges` contains all of `orientation`, `screenSize`, `smallestScreenSize`, `screenLayout`, `keyboard`, `keyboardHidden`, `navigation` and `uiMode`, and that `excludeFromRecents="true"`. Failures name the activity and the missing piece. The `PopupActivity` comment that lists the contract stays and mentions that the test enforces it. Tests that read repo files should resolve the path the way core's `cases/` tests already do.

**Seams:**
- Seam B: manifest contract (JVM test parsing the manifest)

**Blocked by:** None (can start immediately)

Status: ready-for-human

- [x] The test passes against the current manifest (both popup entry points)
- [x] A negative check against a small inline manifest string with `uiMode` removed fails, with a message naming the activity and `uiMode`
- [x] The test finds at least one popup entry point, so it can't pass vacuously if the theme name changes
- [x] Gradle reruns the test when the manifest changes (declare it as a test input, as core does with `cases/`)
- [x] `./gradlew :app:testDebugUnitTest` passes
