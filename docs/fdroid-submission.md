# Submitting Casio Pager to F-Droid

F-Droid builds apps from source and signs them with its own key, so users get updates from F-Droid rather than from
GitHub Releases (the two signatures differ; switching between them means reinstalling). This file is the draft of the
metadata and the checklist for the maintainer. Nothing here has been submitted.

## Before submitting

- [x] The repository is **public** (F-Droid must be able to clone it).
- [x] The release tag `v0.3.0` exists and builds from a clean checkout (checked 2026-10-19: `git clone --branch v0.3.0`,
      `cd android`, `./gradlew assembleRelease` with `ANDROID_HOME` set; signed with the debug key locally, F-Droid
      re-signs). The unsigned build is the same size as the published APK, a good sign for reproducibility.
- [x] `versionName` and `versionCode` in `android/app/build.gradle.kts` are literals matching the tag (they are, and the
      release workflow checks them).
- [x] Store text exists under `fastlane/metadata/android/<locale>/` (title, short and full description, changelog named
      after the versionCode). Add phone screenshots in `fastlane/metadata/android/<locale>/images/phoneScreenshots/`
      (PNG or JPG, 1.png, 2.png, ...) and an icon in `images/icon.png` if you want one in the listing.
- [x] Dependencies are free software only: AndroidX, Jetpack Compose, Kotlin coroutines (and the Guava
      `listenablefuture` stub). No Google Play Services, no analytics or crash-reporting SDKs. The release APK contains
      no play, firebase, gms, crashlytics or analytics classes (checked). `./gradlew :app:dependencies` shows the tree.
- [x] The licence file (`LICENSE`, MIT) is in the repository root.

## Draft `metadata/app.pkonowrocki.casiopager.yml` for fdroiddata

The same text is in `docs/fdroid/app.pkonowrocki.casiopager.yml`, ready to copy.

```yaml
Categories:
  - Connectivity
License: MIT
AuthorName: Piotr Konowrocki
WebSite: https://github.com/pkonowrocki/casio-pager
SourceCode: https://github.com/pkonowrocki/casio-pager
IssueTracker: https://github.com/pkonowrocki/casio-pager/issues
Changelog: https://github.com/pkonowrocki/casio-pager/blob/HEAD/CHANGELOG.md

AutoName: Casio Pager

RepoType: git
Repo: https://github.com/pkonowrocki/casio-pager.git

Builds:
  - versionName: 0.3.0
    versionCode: 300
    commit: v0.3.0
    subdir: android/app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.3.0
CurrentVersionCode: 300
```

The description comes from the fastlane files, so the metadata needs no `Summary` or `Description`. If reviewers ask for
anti-feature labels: the app has none known (no ads, no tracking, no non-free dependencies); the optional diagnostics
push only talks to a server the user sets up.

## Steps for the maintainer

1. Make the repository public and tag the release (`v0.3.0`; see `android/README.md`).
2. Read the [F-Droid inclusion how-to](https://f-droid.org/docs/Inclusion_How-To/) and the
   [fdroiddata contributing guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md).
3. Fork <https://gitlab.com/fdroid/fdroiddata>, add the file above as `metadata/app.pkonowrocki.casiopager.yml`.
4. Check it locally if you have the F-Droid server tools: `fdroid lint app.pkonowrocki.casiopager` and
   `fdroid build -v -l app.pkonowrocki.casiopager`.
5. Open a merge request, answer reviewers (they may ask for changes to the build or the metadata), and wait: review is
   done by volunteers and can take days to weeks.
6. Later releases: with `UpdateCheckMode: Tags` and `AutoUpdateMode: Version`, pushing a new tag is enough once the app
   is listed.

## Known risk

The app compiles against Android SDK 36 with Android Gradle Plugin 8.9.3. If F-Droid's build server does not have that
platform or build tools yet, reviewers will say so in the merge request; the fix is a note in the metadata or waiting for
the server image. `fdroid lint`/`fdroid build` were not run locally (the F-Droid server tools are not installed here).

## Things reviewers may raise

- **Notification access and companion-device permissions** are sensitive; the privacy note and the in-app explanation
  of each permission are the answer.
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** is used because the Bluetooth link service must stay alive; the request is
  optional and explained.
- **`usesCleartextTraffic`** is on so a diagnostics server on a home network can use plain HTTP; nothing is sent unless
  the user opts in.
