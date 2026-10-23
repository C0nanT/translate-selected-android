# Deploy

The app is not on the Play Store. Each version is a signed APK attached to a
[GitHub Release](https://github.com/C0nanT/translate-selected-android/releases),
downloaded and installed straight on the phone/tablet.

```
git tag vX.Y.Z  ->  git push origin vX.Y.Z  ->  workflow "release"  ->  Release vX.Y.Z + APK
```

## How it works

Workflow: [`.github/workflows/release.yml`](../.github/workflows/release.yml). It only runs on a
**push of a tag starting with `v`**; pushes to `main` do not build anything.

1. Checkout, Java 17 (Temurin) and Gradle cache.
2. Unit tests: `./gradlew :core:test :app:testDebugUnitTest`. A failing test stops the release.
3. Decodes the `KEYSTORE_BASE64` secret into a temporary `.jks` and runs `./gradlew assembleRelease`
   with these variables, read by `app/build.gradle.kts`:

   | Variable | Source | Becomes |
   |---|---|---|
   | `VERSION_NAME` | tag without the `v` (`v0.3.0` -> `0.3.0`) | `versionName` (shown in Android settings) |
   | `VERSION_CODE` | `github.run_number` (grows by 1 on every run of the workflow) | `versionCode` (Android only installs over the old one if it is higher) |
   | `KEYSTORE_PATH` | temporary file from `KEYSTORE_BASE64` | release signing key |
   | `KEYSTORE_PASSWORD` | secret | store and key password |
   | `KEY_ALIAS` | not set -> `release` | key alias |

4. Attaches the APK twice to a new Release (notes generated from the commits since the previous tag):
   `translate-selected-vX.Y.Z.apk` (versioned) and `translate-selected.apk` (fixed name, so
   `releases/latest/download/translate-selected.apk` always serves the newest version).

Local builds (`make android`, `make android-apk`) do not use any of this: they produce the
debug APK, versioned `0.1` / `1`, signed with the machine's debug key.

## Releasing a version

```bash
git switch main && git pull
make test                                  # optional: CI runs it too
git tag v0.3.0
git push origin v0.3.0
gh run watch                               # or the Actions tab on GitHub
```

Versioning: [SemVer](https://semver.org/), always with the `v` prefix.

- `fix:` commits only -> patch (`v0.2.0` -> `v0.2.1`)
- some `feat:` -> minor (`v0.2.1` -> `v0.3.0`)
- breaking change (`feat!:` or `BREAKING CHANGE:` footer) -> major

Commits follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat`, `fix`,
`refactor`, `test`, `docs`, `ci`, `chore`; optional scope `core`/`app`), which keeps the generated
release notes readable.

## Installing on the phone

End-user guide in pt-BR: [INSTALL.md](INSTALL.md). Short version:

1. Open https://github.com/C0nanT/translate-selected-android/releases/latest/download/translate-selected.apk on the phone.
2. The download starts right away.
3. Open the file. The first time, Android asks to allow installing from unknown sources for the
   browser/file manager: allow it and go back.
4. Play Protect warns about an unknown app: tap **More details** -> **Install anyway**. The big button
   rejects the install and Android then only says "App not installed".
5. Newer versions install over the old one, keeping the settings (bubble position).

**Once:** an app installed with `make android` (debug key) has a different signature. Uninstall it
before installing the first APK from Releases; the same is true to go back.

## Signing key (one-time setup)

Already done for this repo. Only redo it on a new repo or if the key is lost.

```bash
sh scripts/setup-signing.sh
```

It creates `~/.android-keys/translate-selected-android.jks` (alias `release`), writes the password to
`~/.android-keys/translate-selected-android.txt` and stores the repo secrets:

| Secret | Content |
|---|---|
| `KEYSTORE_BASE64` | the `.jks` in base64 |
| `KEYSTORE_PASSWORD` | store and key password |

**Back up `~/.android-keys/` outside this machine** (password manager, encrypted drive). GitHub
secrets cannot be read back. If the key is lost, a new one can be created with the script (delete
the old file first), but every installed copy must be uninstalled once, because Android refuses an
update signed by another key.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Workflow fails with `missing secret KEYSTORE_BASE64` | secrets not set: run `scripts/setup-signing.sh` |
| Tests fail in CI | fix, commit, then move the tag (below) |
| "App not installed" right after the Play Protect dialog | the dialog's main button rejects: use **More details -> Install anyway** |
| "App not installed" / "conflicts with an existing package" | installed app was signed with another key (debug or old key): uninstall it first |
| Android refuses as a downgrade | the installed `versionCode` is higher: uninstall, or release a new tag (`run_number` always grows) |
| Wrong tag, or the release must be redone | `gh release delete vX.Y.Z --cleanup-tag -y`, then tag and push again |

Moving a tag to another commit without deleting the release:

```bash
git tag -f v0.3.0 <commit>
git push -f origin v0.3.0      # runs the workflow again and replaces the APK
```
