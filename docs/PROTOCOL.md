# Casio ABL-100WE Bluetooth notes

What Casio Pager knows about how the Casio ABL-100WE (module 3565) talks Bluetooth LE, and how it is used. These notes
come from observing the watch with its own app and from the app's behaviour on a phone; no Casio code or firmware is
used or included. Everything below was observed on one watch unless it says otherwise. If you find something that
differs on your watch, please open an issue (see CONTRIBUTING.md; protocol claims need an observation behind them).

Throughout, `<xx>` means a byte in hex, and `AA:BB:CC:DD:EE:FF` stands for a watch's Bluetooth address.

## Summary

- The watch has no notification feature of its own. The only thing that makes it sound on demand is an **immediate
  alert**: write `01 01` to the opcode characteristic while a link is held.
- The watch decides when a link exists. The phone cannot wake it. Only a **long-press of the connect button** produces a
  link that stays up (hours); the watch's own short sessions close within seconds.
- A held link needs a **time write** after connecting and a **keepalive read** every 150 seconds.

## GATT layout

Standard services `0x1800` (name, appearance), `0x1801`, `0x1804` (Tx power). The watch is bonded through Android's
companion-device pairing.

Custom service `26eb000d-b012-49a8-b1f8-394fb2032b0f`. Characteristics are `26eb00XX-b012-49a8-b1f8-394fb2032b0f`:

| XX | Properties | Role |
|----|------------|------|
| 2c | write without response | requests: write `<opcode>`, the watch answers on 2d |
| 2d | notify, write | replies and data; also where settings records are written back |
| 23 | notify, write | bulk-transfer control (Life Log) |
| 24 | notify, write without response | bulk-transfer data (Life Log blob) |
| 2e, 2f | write / notify | present, unused by this app |

The layout matches the G-Shock family as documented by open-source projects such as gshockapi. There is no
`26eb0030`, the characteristic those projects use for phone notifications on other models, so there is no notification
path on this watch.

## Connections: who starts them, and the "kind" byte

The watch always initiates. It advertises in a short burst right before it connects, and Android's standing background
connect request (`connectGatt` with `autoConnect`) picks it up within a second. The reply to opcode `10` carries a
**connection kind** in byte 7:

| kind | Trigger | Behaviour |
|------|---------|-----------|
| `01` | long-press of the connect button | holds as long as the keepalive is answered (seen: minutes to 4.4 hours) |
| `03` | the watch's automatic windows at 00:30, 06:30, 12:30 and 18:30 | the watch hangs up as soon as setup and the time write are done (about 8 s) |
| `04` | a short press of the search/connect button | same as `03` |

In the `03` and `04` sessions the app has time for the init sequence and the clock write, not for anything more: a Life
Log read attempted right after the init found the link already closed in every window logged. A long hold ends when the
watch hangs up (Android status 257, remote user terminated) or, less often, with a timeout (status 147). The cause of the
hang-up is not known.

"Connection time with the app" (3 / 5 / 10 minutes, record `11`, last byte) does not cap a held link: links kept going
past ten minutes with the setting at 5. Some shorter holds of about 5 minutes were seen too, so the setting may matter
in situations not yet understood.

## Opcodes seen

Requests are written to 2c as `<opcode>`; the watch replies on 2d as `<opcode> <data>`. Settings are written back to
2d as `<opcode> <data>`.

| Opcode | Meaning |
|--------|---------|
| `22`, `10`, `11`, `26`, `28`, `20`, `1d`, `1e` | handshake / info reads sent on every connect. The phone also writes its name as `23 <name>` |
| `09 <time>` | **time write**: year (2 bytes) month day hour min sec, weekday and a trailing byte. Without it the watch drops the link about 69 s after connecting |
| `11` | connection record; last byte = connection time in minutes (`0a`, `05`, `03`); bytes 12-13 hold the first automatic window (`00 1e` = 00:30) |
| `13` | the basic settings record, 12 bytes. See below |
| `15` / `16` | alarms. Alarm 1 write: `15 40 40 HH MM` (enabled, constant, hour, minute); disabled is `15 00 00 00 00` |
| `18 hh mm ss` + 11 bytes | timer record (15 bytes in all) |
| `28` | a static record (`28 1d 19 00 00 00 02 00 00` every time); no battery or temperature in it |
| `01 01` | **immediate alert**: a short series of beeps. This is what the app sends for every alert |
| `01 02` | accepted, but a phone recording (2026-10-15) shows the same seven short beeps at about 8.2 kHz as `01 01`; the app does not use it |
| `0a 02` / `0a 00` | watch to phone: Find Phone start / stop. There is no command to make the watch ring beyond the alert above |
| `07` | NEW_ALERT with text: sent without error, no visible or audible effect on this watch |

The immediate alert has no reply packet and no delay. `00 <level>` (link-loss alert level) was not tested.

### Basic settings record (`13`)

Twelve bytes: the opcode (byte 0) plus 11 data bytes. A change is a read-modify-write of the whole record to 2d. The
bits the app uses (offsets count the opcode as byte 0, as in the open-source library):

| Where | Meaning |
|-------|---------|
| byte 1, bit `0x01` | 24 hour display (set = 24 h) |
| byte 1, bit `0x02` | button sound (a **clear** bit means the sound is ON) |
| byte 1, bit `0x10` | power saving (a **clear** bit means power saving is ON; matches the watch's default) |
| byte 2, bit `0x01` | light duration: set = long (3 s), clear = short (1.5 s) |

The other bits are left exactly as read. Power saving is a setting: in a dark place between 10 pm and 6 am the display blanks. There is no command that puts the
watch to sleep immediately.

## Life Log (step counter)

1. Write `00 11 00 00 00` to characteristic 23.
2. The watch announces a 400-byte blob and sends it on characteristic 24.
3. Close with `04 11 00 00 00` on 23. Without the close, only the first read per link works; later reads get silence
   until a close is sent.

The blob starts with a BCD timestamp (year, month, day, hour, minute, second). It holds today's step and distance
counters, hourly buckets for today, and seven stored days. Today's total matched the watch's display. The app reads it
right after a long-press link and every 20 minutes while the link is held, and keeps a daily history on the phone.

## Held link recipe

1. Connect (`autoConnect`), enable notifications on 2d/23/24, run the init reads, write the name, write the **time
   (`09`)**.
2. Read `11` every 150 seconds as the keepalive (120-180 s was fine; the 150 s interval has held links for hours).
3. Send `01 01` to 2d for each alert.

Polling the Life Log instead of `11` as the keepalive was tried and removed.

## Things that did not work

- Alarm tricks to ring the watch from the phone: an alarm at the next full minute works (10-70 s latency) but is not
  immediate. Jumping the clock to just before the alarm did not ring.
- Waking the watch from the phone: not possible as far as observed. The watch advertises only before it connects by
  itself, and the standing connect request is already in place. Untested: a direct (non-auto) connect at a random moment.
- Reading the time back with `09` returns an error packet (`ff 81 09`).
