# Casio Pager (Android project)

Kotlin, Jetpack Compose (Material 3). Package `app.pkonowrocki.casiopager`. compileSdk and targetSdk 36, minSdk 33.
The product description is in the [root README](../README.md); this file is for building and working on the app.

## Build and test

Needs JDK 17 and the Android SDK (platform 36, build-tools 35). Create `android/local.properties` with
`sdk.dir=<path to your SDK>` unless `ANDROID_HOME` is set.

```
cd android
./gradlew testDebugUnitTest          # JVM unit tests, no emulator needed
./gradlew lintDebug                  # Android lint (config in app/lint.xml)
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell monkey -p app.pkonowrocki.casiopager -c android.intent.category.LAUNCHER 1
adb -s <serial> logcat -s CasioPager
```

On Windows with Android Studio, set `JAVA_HOME` to its bundled JDK first:
`export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (Git Bash).

Only one client can hold the watch: uninstall or disable Casio's own app first.

## Using the app

Set-up steps for users are in [docs/INSTALL.md](../docs/INSTALL.md). For development: the first-run screen and the
Status checklist both walk through pairing, permissions, notification access and the battery exemption.

### Reading what happened (the log)

The app keeps a rolling log on the phone (two files of 512 KB, about a day of normal use). It never contains message
text or contact names: only link events (held, closed with the Bluetooth reason and duration), a heartbeat with the
phone's battery level every keepalive, each alert decision with the reason for staying silent, presence wake-ups,
service start and stop, and Bluetooth on or off. Step numbers and the diagnostics URL are never logged.

- In the app: Settings > **Logs** (Share sends the text anywhere, Clear empties it). Settings > **Recent decisions**
  shows the last 30 verdicts.
- Over adb (debug builds): `adb shell run-as app.pkonowrocki.casiopager cat files/pager.log.1 files/pager.log`

Reasons for a silent message, as they appear in the log: `no-rule`, `group`, `duplicate`, `throttle`, `quiet-hours`,
`call-repeat-gap`, `blank-sender`, `app-off`. A notification from a messaging or dialer app that was dropped before
the rules are consulted shows as `unmapped pkg=...` (group summaries, ongoing calls, system notifications).

### Optional diagnostics server (Loki)

Off by default and hidden: tap the version row in Settings seven times to unlock **Diagnostics**. To send the log to
your own [Grafana Loki](https://grafana.com/oss/loki/), open a setup link on the phone:
`casiopager://diagnostics?url=<percent-encoded Loki base URL>` (type it into a note or scan a QR code, then tap it). The
app asks "Send diagnostics to <host>?" (only the host is shown, because the path may be a secret) and, on Enable, pushes
the same log lines to `<url>/loki/api/v1/push`. Diagnostics > Send diagnostics switches it on and off; Remove forgets the
server.

A build can start with a server by setting the `LOKI_URL` environment variable when running Gradle (it becomes
`BuildConfig.LOKI_URL`). Do not publish such a build: anyone with the APK can read the URL.

## Quick Settings tile

Swipe down twice, tap the pencil (edit) and drag **Casio Pager** into the visible area. It is lit while a long-press link
is held and shows how long ("Linked · 1 h 20 min"); otherwise it says "Long-press the watch", "Not paired", "Setup
needed" or "Tap to start".

## Releases

[`ci.yml`](../.github/workflows/ci.yml) runs the unit tests and lint and builds a debug APK on every push to `main` and
every pull request. [`release.yml`](../.github/workflows/release.yml) builds, signs and publishes a release when a tag
`v<major>.<minor>.<patch>` is pushed.

To release:

1. Update `CHANGELOG.md`, add `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.
2. Bump `versionName` and `versionCode` in `app/build.gradle.kts` together. `versionCode` is
   `major * 10000 + minor * 100 + patch` (0.3.0 is 300). The workflow fails if they do not match the tag.
3. Commit, then `git tag v<version>` and `git push origin v<version>`.

The workflow publishes `casio-pager-v<version>.apk` and `SHA256SUMS` on the Releases page. It refuses to publish without
the signing key, so it never produces a debug-signed release. Repository secrets it needs:

- `ANDROID_KEYSTORE_B64`: the keystore file, base64 encoded (`base64 -w0 release.keystore`)
- `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`

Create the keystore once and keep it safe (without it you cannot publish updates that install over existing apps):
`keytool -genkeypair -v -keystore release.keystore -alias casiopager -keyalg RSA -keysize 4096 -validity 10000`.
A local `assembleRelease` without `ANDROID_KEYSTORE_PATH` is signed with the debug key; it installs, but cannot update an
app signed with the real key, or the other way round.

F-Droid builds the app from the tagged source and signs it with its own key; see
[docs/fdroid-submission.md](../docs/fdroid-submission.md).
