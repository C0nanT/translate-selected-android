# Tech debt review index

One module is reviewed per run of `/tech-debt-map`. Reports are working material in `.scratch/tech-debt-map/<module>/<date>.md`. This index is committed so the team can see when each module was last looked at.

| Module | Paths | Origin | Last review | Report | Open |
| ------ | ----- | ------ | ----------- | ------ | ---- |
| app | `app/**` | declared | 2026-10-23 | `.scratch/tech-debt-map/app/2026-10-23.md` | 7 |
| core | `core/**` | declared | never | — | — |

## Findings ledger

### app

| # | Finding | First seen | Status |
| - | ------- | ---------- | ------ |
| 1 | QS tile enables bubble without POST_NOTIFICATIONS request; toggle decision duplicated | 2026-10-23 | resolved (7faa6c4) |
| 2 | Translate-once-per-open relies on hand-copied `configChanges`, checked only by a comment | 2026-10-23 | resolved (07699f9) |
| 3 | Unclassified network failures and blocked clipboard reads never logged | 2026-10-23 | resolved (408084d) |
| 4 | One-popup-per-tap beyond 500 ms left to unspecified task/affinity behaviour | 2026-10-23 | open |
| 5 | Request task throwing an `Error` leaves popup stuck on "Traduzindo…" | 2026-10-23 | open |
| 6 | Unguarded `wm.addView` in a `START_STICKY` service can crash-loop | 2026-10-23 | open |
| 7 | `HttpFetcher` has no connection seam; cancel path untestable | 2026-10-23 | open |
| 8 | Drag/long-press state machines duplicated in touch callbacks, untested | 2026-10-23 | open |
| 9 | User-facing outcome text split between app and core; `cases/` doesn't pin it | 2026-10-23 | open |
| 10 | Release build has no R8/minify or resource shrinking | 2026-10-23 | open |

## Guardrails

- Every popup entry point's manifest contract (`@style/Popup`, full `configChanges`, `excludeFromRecents`) is checked by a unit test that parses `AndroidManifest.xml`, not by a comment.
- Every swallowed or converted exception in `app/` gets a `Log.w` with the throwable, even when the user-facing message stays the same.
- Work submitted to `Requests.executor` always posts a result back (catch `Throwable` at the task boundary). A popup must never be left on "Traduzindo…".
- User-facing outcome text that the README promises is pinned in `cases/*.json`, so app, core and the PC twin can't drift.
