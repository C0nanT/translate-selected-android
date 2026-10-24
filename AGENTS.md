## SOLID

Apply SOLID at the **architecture** level: module boundaries, dependency direction, and the interfaces between them. It is a way to shape seams, not a naming ritual. "Module" means whatever this codebase groups behaviour into: a class, a package, a file of functions, a service.

### Scope: boy scout rule

SOLID applies to:

- code written new in the current change, and
- the existing code the current flow already passes through, when a small local edit clears friction that change is hitting.

The rest of the codebase stays as it is. Keep a change's blast radius on the flow being built or fixed: a repo-wide SOLID refactor is its own piece of work, and happens only when explicitly asked for. The codebase converges one change at a time.

When applying a principle would require reshaping modules outside the current flow, leave them alone and say so in the summary of the change.

### In this repo

- **Policy**: `core/src/main/kotlin/**` (Gradle module `:core`): pure Kotlin/JVM rules (`translate`, `joinLines`, `Result`, `NetworkError`, `Stopwatch`, `clampPosition`, limits). No Android plugin: an `android.*` import fails compilation, on purpose. Also pure, Android-free classes inside `app/` such as `Taps`.
- **Details**: `app/src/main/java/**` (Gradle module `:app`): Activities, `BubbleService`, `BubbleTile`, `WindowManager`/clipboard/prefs access, and `HttpFetcher` (`HttpURLConnection`) in `Network.kt`, which converts platform exceptions into core's `NetworkError`.
- **Wiring**: plain function parameters, no DI container. Core takes its IO as a function (`translate(raw, fetch: (String) -> String)`, called as `translate(text, fetcher::fetch)` from `PopupActivity`) and time as an argument (`Stopwatch`/`Taps` receive "now" in ms on each call). The error contract is defined by core: the adapter throws `NetworkError`.
- **Test substitution**: JVM unit tests (JUnit 4) pass a lambda in place of `fetch` (`core/src/test/.../TranslatorTest.kt`) and plain `Long` timestamps in place of the clock. Behaviour shared with the PC twin (`pc/core.py`) is pinned by data-driven cases in `cases/*.json`, read by tests in both repos.

### The principles, as architecture rules

- **SRP**: a module has one reason to change. When one flow forces edits in a module that other flows also own for unrelated reasons, that module is holding two responsibilities.
- **OCP**: new behaviour arrives as a new implementation behind an existing interface, rather than another branch in a growing conditional over kinds of thing.
- **LSP**: every implementation of an interface is substitutable through that interface: same contract, same error behaviour, no "this one also needs X called first".
- **ISP**: a consumer depends on the narrow interface it actually uses. Interfaces are shaped by the caller's need, not by everything the implementation can do.
- **DIP**: policy does not depend on details (see *In this repo* above for both). The interface belongs to the policy side; the detail implements it and is passed in.

### Applying it

- When a new flow crosses an IO boundary, define the interface from the policy side and inject the implementation.
- One production implementation is enough **when a test substitutes it**: the test double is the second implementation, and the interface is the test surface. An adapter behind an interface with a single caller and no substitution is a hypothetical seam: drop the interface until something real needs it.
