# Installing and setting up Casio Pager

You need a Casio ABL-100WE (module 3565) and an Android 13 or newer phone.

## 1. Install the app

1. Open the [Releases](https://github.com/pkonowrocki/casio-pager/releases) page on your phone and download
   `casio-pager-v<version>.apk`. Optionally check it against `SHA256SUMS` from the same release:
   `sha256sum -c SHA256SUMS` on a computer, or compare the hash with any checksum app.
2. Open the downloaded file. Android asks to allow installing apps from this source (your browser or file manager):
   allow it for that app only. The setting is under **Settings > Apps > Special app access > Install unknown apps**.
3. Tap **Install**. Updates from later releases install over the existing app, because every release is signed with the
   same key.

## 2. Remove Casio's own app

Only one app can hold the watch at a time. Uninstall or disable **Casio Watches** (the official app), or the two will
fight over the link.

## 3. First run

The first-run screen lists four steps, each with a one-line reason. Do them in this order:

1. **Allow Bluetooth and notifications.** Bluetooth talks to the watch; notifications show the small status bar the
   app needs while it runs.
2. **Allow notification access.** The app reads incoming notifications on your phone to decide when to beep the watch.
   It never stores or sends message text. Android opens the notification-access list: turn on **Casio Pager**.
   - If the switch is greyed out ("Restricted setting"): open **Settings > Apps > Casio Pager**, tap the three-dot menu
     and choose **Allow restricted settings**, then try again.
3. **Pair your watch.** Tap *Set up* next to "Pair your watch" and, while the list is open, **long-press the watch's
   connect button**. Pick "CASIO ABL-100WE" in the list. From then on the app
   talks to that one watch only and starts by itself when you long-press it.
4. **Don't let Android stop the app (battery).** Allow the app to run in the background without battery optimisation.
   Without this Android may stop the link service after a while.

You can skip the screen and do the same steps later from the Status screen.

## 4. Choose who can beep the watch

Open **Rules**. Each app is a card with three modes: **Off**, **Everyone**, or **Some people** (only the people you add).
Under "Some people", tap a name in *Recently seen* to add them. Anything you leave alone stays silent. Group chats have
their own section in each card. **Settings** has quiet hours and the quiet period after a beep.

## 5. Connect the watch

Long-press the watch's connect button. The Status screen turns green ("Linked") within a few seconds. Use **Test beep**
to check the watch sounds. The link stays up for hours; when it drops, long-press again. Alerts only reach the watch
while it is linked.

Optional: add the **Casio Pager** tile to Quick Settings (swipe down twice, tap the pencil, drag the tile into view). It
is lit while the link is held and shows how long.

## Troubleshooting

- **The Status card names a problem.** The "Is it working?" card shows the single most important thing to fix and a
  button for it.
- **Nothing beeps.** Open Settings > Help > **Recent decisions**: it shows what happened to the last messages ("No rule
  for this sender", "Quiet hours", "Should beep, but the watch was not linked", ...).
- **"Watch not linked".** Long-press the watch's connect button. The phone cannot wake the watch, so this is the only
  way to start a link.
- **The link keeps dropping.** The watch ends links itself. Check the app is exempt from battery optimisation, Bluetooth
  is on, and the phone is near the watch. Settings > Logs shows why each link ended.
- **Notification access is greyed out.** Use *Allow restricted settings* as in step 3.2.
- **Pairing finds nothing.** Long-press the watch while the picker is open, and keep the phone close.
- **Reporting a bug.** Settings > Logs > **Share** produces a log that contains no message text or names. Attach it to
  a [GitHub issue](https://github.com/pkonowrocki/casio-pager/issues/new/choose).

## Uninstalling

Uninstall like any app. The app stores its settings and history on the phone only, and cloud backup is off, so
nothing is left elsewhere.
