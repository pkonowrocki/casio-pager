# Contributing

Thanks for helping. This is a small, single-maintainer project, so keep changes focused and explain the why.

## Build and test

You need JDK 17 and the Android SDK (platform 36 and build-tools 35). From the repository root:

```
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows with Android Studio installed, point Gradle at its bundled JDK first:
`export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (Git Bash).

CI runs the same three tasks on every push and pull request. A change should keep tests and lint green.

## Code style

- Kotlin, official style, Jetpack Compose with Material 3. Match the surrounding code: naming, comment density, idiom.
- Keep logic that can be tested as plain functions (presenters, parsers, codecs) out of composables, and unit-test it.
  Presenters return `@StringRes` ids, not text, so they stay testable and translatable.
- Prefer small changes: no new dependency for something a few lines of platform code can do.
- Never log message text, contact or group names, step numbers or a diagnostics URL. The log is meant to be shareable.

## Adding or fixing a language

All user-visible text lives in `android/app/src/main/res/values*/strings.xml`. English (`values/`) is the default;
German, French, Spanish and Polish are `values-de`, `values-fr`, `values-es` and `values-pl`.

1. Copy `values/strings.xml` to `values-<code>/strings.xml` and translate the values, not the names.
2. Use the typographic apostrophe (`’`) or escape a plain one (`\'`) inside strings, or the build fails.
3. Keep placeholders (`%1$s`, `%1$d`) and use plurals for counts.
4. Add the language to `res/xml/locales_config.xml` so it shows in Android's per-app language picker.

## Reporting a bug

Open an issue and include the app version, phone model and Android version, and what you did. Attach the log from
**Settings > Logs > Share**; it contains no message text or names. For link problems, say whether you long-pressed the
watch's connect button and how long the link held.

## Protocol changes

Claims about how the watch behaves must be backed by an observation: a captured Bluetooth trace or a logged session,
with the situation described. Put verified findings in [docs/PROTOCOL.md](docs/PROTOCOL.md) and say what is observed
versus guessed. Do not paste Casio's code, resources or firmware images into the repository, and do not decompile and
copy: describing what you observed on the wire is enough.

## Pull requests

- One logical change per pull request, with a short description of what and why.
- Small commits with clear messages. Do not commit APKs, keystores, secrets or build output.
- No `Co-Authored-By` trailers unless the co-author is a person who agreed to it.
- By contributing you agree that your work is licensed under the project's [MIT licence](LICENSE).
