# Guidance for AI coding agents

Casio Pager is an Android app (Kotlin, Jetpack Compose) that beeps a Casio ABL-100WE watch for chosen notifications.
Read `README.md` for the product and `docs/PROTOCOL.md` before touching Bluetooth code.

## Layout

```
android/                       Gradle project (run Gradle from here)
  app/src/main/java/app/pkonowrocki/casiopager/
    engine/    alert decisions (AlertEngine), ring repeater, decision history
    link/      Bluetooth: GattClient, WatchSession (init, keepalive, alert, Life Log), services
    log/       rolling log, optional Loki push, diagnostics setup link parser
    mapping/   notification -> event (NotificationMapper)
    model/     Settings, Rule, AppRule and friends, WatchAddress
    protocol/  byte-level records (settings, Life Log, timer) and packet builders
    service/   notification listener
    storage/   settings codec/store, steps history
    tile/      Quick Settings tile
    ui/        screens, presenters, onboarding, theme
  app/src/main/res/            strings in values/ (en), values-de/-fr/-es/-pl
  app/src/test/                JVM unit tests (no emulator needed)
docs/                          PROTOCOL.md (watch behaviour), INSTALL.md, ANDROID-NOTES.md
fastlane/metadata/android/     F-Droid / store listing text
.github/workflows/             ci.yml (tests, lint, debug APK), release.yml (signed APK from a v* tag)
```

## Build and test

```
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Needs JDK 17 and Android SDK platform 36 + build-tools 35. On Windows with Android Studio, use its JDK:
`export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (Git Bash). On Linux any JDK 17 works.
Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Run tests and lint before you say a change is done.

## Conventions

- Keep logic out of composables. Presenters and mappers are pure functions with unit tests; presenters return
  `@StringRes` ids (plus format arguments), composables turn them into text with `stringResource`.
- Every user-visible string goes in `values/strings.xml` **and** the four translations (`values-de`, `-fr`, `-es`,
  `-pl`). Use the typographic apostrophe `’` in XML (a plain `'` must be written `\'`). Use plurals for counts.
- **Never log** message text, contact or group names, step numbers, or the diagnostics URL or its path. Log lines
  carry reasons and app keys only. The log is shared by users in bug reports.
- Settings are stored as JSON by `SettingsCodec`; keep decoding tolerant (old saved settings must still load) and add a
  test for every field you add or retire.
- Don't add dependencies for something a few lines of platform code can do. Dependencies must be free software
  (the project is aimed at F-Droid): no Google Play Services, no analytics or crash-reporting SDKs.
- Protocol claims need an observation (a captured trace or a logged session). Say what was observed and what is a guess
  in `docs/PROTOCOL.md`. Never add Casio's code, resources or firmware images.
- The watch controls connections; the phone cannot wake it. Don't promise alerts without a held link.

## Git and safety

- Small, focused commits with clear messages. **No commit trailers** (no `Co-Authored-By`, no session links).
- Stage files by explicit path; never `git add -A` or `git add .`.
- Never commit `*.apk`, `*.apkm`, `*.xapk`, keystores, `local.properties`, tokens or any secret. Release signing keys
  live only in repository secrets (`ANDROID_KEYSTORE_B64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
  `ANDROID_KEY_PASSWORD`).
- Versions: bump `versionName` and `versionCode` (major*10000 + minor*100 + patch, so 0.3.0 is 300) in
  `android/app/build.gradle.kts` before tagging `v<major>.<minor>.<patch>`; the release workflow checks them against
  the tag. Add a `fastlane/metadata/android/*/changelogs/<versionCode>.txt` for each release and update `CHANGELOG.md`.
- Do not publish or release anything (tags, public visibility, store submissions) unless the maintainer asked for it.
