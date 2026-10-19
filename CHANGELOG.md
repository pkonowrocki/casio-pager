# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
The Android `versionCode` is `major * 10000 + minor * 100 + patch` (0.3.0 is 300).

## [Unreleased]

## [0.3.1] - 2026-10-19

A packaging release for F-Droid; no change in behaviour.

### Changed
- The release build is shrunk with R8 (code and resources): the APK is about 2 MB instead of 17 MB.
- The extra "dependency metadata" block that the Android Gradle Plugin adds to the APK signing block is turned off,
  as F-Droid requires.

## [0.3.0] - 2026-10-19

The first public release.

### Added
- Group chats can be allowed one by one per app, matched by chat title. The old all-or-nothing switch remains.
- **Recent decisions** screen (Settings > Help): the last 30 verdicts with a plain reason, without names or text.
- "Is it working?" card on Status: a green tick or the single most important problem with a button to fix it.
- Quick Settings tile showing whether a long-press link is held and for how long.
- Steps are read on every link and every 20 minutes while linked; a daily history is kept on the phone and shown on
  the Watch tab even with no link. The Watch tab also shows when the clock was last synced.
- More messengers as named sections: Signal, Viber, Discord, Slack, Microsoft Teams, Google Chat, Gmail, Outlook,
  Instagram, Skype, Threema. Other apps are listed by their real names, sorted, and folded away.
- Diagnostics are opt-in: a one-tap setup link (`casiopager://diagnostics?url=...`) with a confirmation that shows only
  the server host, and an on/off switch. The server is never shown in full or logged.
- MIT licence, privacy note, install guide and contribution docs.

### Changed
- Minimum Android version is now 13 (SDK 33); the app targets Android 16 (SDK 36).
- One beep instead of chirp and two-tone: they sound identical on the watch. The app sends `01 01`.
- Status hints say plainly that alerts wait for the long-press. One primary action on Status; navigation icons;
  seconds instead of milliseconds for timing settings; larger text sizes and landscape handled; system bar icons follow
  the theme.
- The watch address moved behind Diagnostics; Settings shows only "Paired" or "Not paired". Logs live under
  Settings > Help. The quiet period after a beep defaults to 30 seconds.
- Cloud backup is off, so the watch address and any diagnostics server are never backed up or restored.

### Fixed
- Starting the link from a presence wake without the Bluetooth permission crashed the app on Android 14+.
- Zero-width and bidirectional control characters are stripped from names by escape sequence instead of literal
  characters; the companion-device pairing call is only used on Android 13+.

## [0.2.0] - 2026-10-15

Private pre-release.

### Added
- Rules as a tree per app: off, everyone, or only the people you pick, for WhatsApp, Messenger, SMS, calls, Telegram
  and any other app that posts notifications.
- Translations: English, Deutsch, Français, Español, Polski; first-run permissions screen.
- Life Log steps, timer and connection time on the Watch tab; watch settings (24 h, button sound, light duration,
  power saving) read and changed from the phone.
- Companion-device presence: the app starts itself when the watch advertises (a long-press or an automatic window).
- Rolling on-device log with ignore reasons, optional diagnostics push, quiet hours, call repeat, throttle.
- CI that builds and tests, and publishes a signed release APK from a tag.
