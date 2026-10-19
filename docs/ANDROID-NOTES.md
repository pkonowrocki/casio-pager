# Android notes

Developed and tested on a Pixel 7 running Android 17 (SDK 37). Built with compileSdk and targetSdk 36; minSdk is 33
(Android 13), because the companion-device association and presence APIs the app depends on need it. Other phones and
Android versions are untested.

## Companion presence auto-start

Pairing uses Android's companion-device manager. Setup > "Set up" next to "Pair your watch" (long-press the watch's
connect button while the picker is open, pick "CASIO ABL-100WE"). The app then calls `startObservingDevicePresence`.
From then on the system wakes `PresenceService` whenever the watch advertises: on a long-press, and at the watch's
four automatic windows. `WatchLinkService` starts in the foreground and connects.

The wake works with the app closed, and after `adb shell am force-stop app.pkonowrocki.casiopager`:

    presence: watch appeared
    service started
    link held kind=1 initTimeouts=0
    presence: watch disappeared     (the watch stops advertising once connected: expected)

Two `appeared` callbacks within half a second are normal; `WatchLinkService.onStartCommand` is idempotent. The wake
checks the Bluetooth permission first, because starting a `connectedDevice` foreground service without it throws.

Not tested: reboot persistence, long idle (doze), other Android versions, other watches.

## Useful commands

- `adb shell cmd notification allow_listener app.pkonowrocki.casiopager/app.pkonowrocki.casiopager.service.PagerNotificationListener`
  grants notification access without the settings screen.
- `adb shell run-as app.pkonowrocki.casiopager cat files/pager.log` prints the app's rolling log (debug builds).

## Observed notification shapes

WhatsApp and Messenger one-to-one chats use MessagingStyle with the sender in the message. An incoming call is posted
two or three times within about 50 ms (the engine's call repeat gap turns that into one beep). SMS (Google Messages) is
MessagingStyle. Group chats are recognised by the notification's group flag; the chat title comes from the conversation
title, or the main title when that is missing.
