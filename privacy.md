<div align="center">

# PRIVACY POLICY

<sub>english · <a href="./privacy.es.md">castellano</a> · <a href="./readme.md">readme</a> · <a href="./permissions.md">permissions</a> · <a href="./security.md">security</a></sub>

</div>

---

<!-- TODO(owner): legal review (gdpr / lopdgdd) before treating this policy as final. -->

| | |
|---|---|
| applies to | the focusphone android app (`app.focusphone`), version `1.4.0-beta` and later |
| last updated | 2026-11-06 |
| author | Juan García Cazallas ("rezzt-dev") |
| contact | `contacto@rezzt.dev` |

this policy describes what the app does with data. it does not cover the website
<https://focusphone.rezzt.dev>, which has its own privacy page, or github, which hosts this
repository under the [github general privacy statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).
it is published in english and in spanish ([`privacy.es.md`](./privacy.es.md)); if they
differ, the spanish version prevails, as in the [`license`](./license).

---

## 1. SUMMARY

- **nothing leaves your device.** the app declares no `INTERNET` permission, so android
  does not let it open a network connection. there is no server, no account, no sync, no
  advertising, no analytics, no crash reporting and no remote configuration.
- **the author receives no data from the app.** the author cannot see who uses focusphone,
  how, or on which device.
- **what the app stores, it stores in its private storage**, and the parts that reveal how
  you use your phone are encrypted with a key held by the android keystore.
- **there are no backups**: the app's data is excluded from cloud backup, `adb backup` and
  device-to-device transfer.

---

## 2. DATA STORED ON THE DEVICE

everything below lives in the app's private storage (`/data/data/app.focusphone/`), which
other apps cannot read. "encrypted" means aes-256/gcm with a key generated and kept by the
android keystore, which never leaves it.

| data | where | encrypted |
|---|---|---|
| favourite apps and their order | room database `focusphone_database` | no |
| theme, typeface, text sizes, alignment, clock style, gestures, tutorial state | shared preferences | no |
| home screen grid (size, blocks, widget ids and their cells) | shared preferences | no |
| conscious pause settings, notification panel settings | shared preferences | no |
| wallpaper settings | shared preferences | no |
| wallpaper images you pick | a downscaled copy in the app's files (`wallpapers/`) | no |
| world clock cities chosen for a widget | shared preferences | no |
| **intention text** of the intention widget | shared preferences | **yes** |
| **custom app names** | shared preferences | **yes** |
| **drawer folders** and their apps | shared preferences | **yes** |
| **active profile, detox timer, blocked apps, work keywords** | shared preferences | **yes** |
| **app schedules, daily limits and usage counters** | shared preferences | **yes** |
| **detox and schedule passwords** | shared preferences | the password is never stored, only a pbkdf2-hmac-sha256 digest (100 000 iterations, random 128-bit salt) |
| failed password attempts and cooldown | shared preferences | **yes** |

the data is kept until you change it, clear the app's storage in android settings, or
uninstall the app, which deletes all of it.

---

## 3. DATA THE APP READS BUT DOES NOT STORE

| data | why | requires |
|---|---|---|
| the list of installed apps (names, package names, icons) | to show the drawer, the favourites selector and the widget picker | `QUERY_ALL_PACKAGES` |
| the app currently in the foreground (its package name only) | to apply profile blocks, schedules and daily limits to apps opened outside focusphone, and to count usage time | the optional accessibility service |
| the notifications currently shown by android | to list them in the side panel and let you dismiss them; kept in memory only and forgotten when access is revoked | the optional notification access |
| what is playing in media apps (title, artist, artwork, playback state) | to show it in the music widgets, with controls | the optional notification access |
| battery level, free storage, memory use, next alarm and torch state | to show them in the system widgets (the wi-fi, bluetooth and volume buttons only open android's own panels) | nothing additional |

the accessibility service reads only the package name of the active window. it does not
read text, take screenshots, type, or press anything for you.

---

## 4. WIDGETS IN OTHER LAUNCHERS

focusphone's 20 widgets can be placed in any launcher. android renders a widget inside the
launcher that hosts it, so that launcher receives what the widget displays (for example
your favourite apps, your intention text, the time in your chosen cities or the song that
is playing). focusphone sends nothing else to it. what that launcher does with it is
governed by its own policy.

---

## 5. PERMISSIONS

| permission or service | granted | what happens without it |
|---|---|---|
| `QUERY_ALL_PACKAGES` | at install | the launcher cannot list your apps |
| `REQUEST_DELETE_PACKAGES` | at install | the app cannot open android's uninstall dialog; android always asks you to confirm |
| `EXPAND_STATUS_BAR` | at install | on older android versions, swiping down cannot open the notification shade |
| accessibility service | optional, by you in android settings | blocks and schedules only apply to apps opened from focusphone; daily limits are not measured |
| notification access | optional, by you in android settings | no notification panel and no music widgets |

the technical justification of each one is in [`permissions.md`](./permissions.md). you can
revoke the two optional services at any time in android settings.

---

## 6. BACKUPS AND CHANGING DEVICES

`android:allowBackup="false"` disables android cloud backup and `adb backup`, and
`res/xml/data_extraction_rules.xml` excludes every data domain from cloud backup and from
device-to-device transfer on android 12 and later. the encrypted values could not be read
on another device anyway, because the keystore key cannot leave the original one. when you
reinstall the app or change phones, you will need to configure it again.

---

## 7. DIAGNOSTIC LOGS

release builds remove debug and informational log messages. warnings and errors are still
written to android's system log and may include the package name of an app when an
operation on it fails (for example, if an app cannot be launched). the system log stays on
the device, is not read or sent by focusphone, and other apps cannot read it.

---

## 8. YOUR RIGHTS

because the app sends no personal data to the author or to anyone else, the author does
not hold any of your app data and cannot access, correct or delete it. you control it
directly: change it in the app, clear the app's storage in android settings, or uninstall
the app.

if you contact the author (by email or through a github issue), the author receives the
data you choose to send — for example your email address, your github user and what you
write — and uses it only to answer you and, for bug reports, to fix the issue. it is kept
only as long as the conversation or the issue needs it. under the eu general data
protection regulation (gdpr) and spain's organic law 3/2018 (lopdgdd), where they apply,
you may ask for access to, rectification, erasure, restriction or portability of that data,
or object to its processing, by writing to `contacto@rezzt.dev`. you also have the right
to lodge a complaint with a supervisory authority, in spain the agencia española de
protección de datos (<https://www.aepd.es>).

---

## 9. CHILDREN

the app does not collect data from anyone, including children. it is not designed for
parental control or for supervising another person (see [`disclaimer.md`](./disclaimer.md)).

---

## 10. CHANGES TO THIS POLICY

changes are published in this file, with a new "last updated" date, and recorded in
[`changelog.md`](./changelog.md). if a future version ever needed to send data off the
device, this policy would be updated before that version is released.

---

<div align="center">

<sub>focusphone — developed by <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
