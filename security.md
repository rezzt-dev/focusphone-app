<div align="center">

# SECURITY POLICY

<sub><a href="./readme.md">readme</a> · <a href="./documentation/en/developers/08-security.md">full security documentation</a> · <a href="./documentation/es/desarrolladores/08-seguridad.md">castellano</a></sub>

<br>

<img src="https://img.shields.io/badge/100%25-offline-4c1?style=flat-square" alt="100% offline" />
<img src="https://img.shields.io/badge/no-trackers-4c1?style=flat-square" alt="no trackers" />
<img src="https://img.shields.io/badge/reports-private%20advisories-000000?style=flat-square" alt="private advisories" />

</div>

---

## SUPPORTED VERSIONS

focusphone is in alpha and there is a single supported line: the latest published
version. there are no long-term support branches and no backports to older alphas.

| version | supported |
|---|---|
| `1.0.6` (latest alpha) | yes |
| any earlier alpha | no — update first |

before reporting, make sure the issue still reproduces on the latest code in
`main`. `versionName` lives in `focusphone-project/app/build.gradle.kts` and the
app shows it in **settings → about**.

---

## REPORTING A VULNERABILITY

**do not open a public issue for a security problem.** use one of these two
channels instead:

| channel | when |
|---|---|
| [github private vulnerability reporting](https://github.com/rezzt-dev/focusphone-app/security/advisories/new) | preferred: the report stays private until a fix is published |
| `privacy@rezzt.dev` | if private advisories are unavailable to you, or you prefer email |

if you email, the subject line `[focusphone security]` helps it not get lost.

### WHAT TO INCLUDE

- the affected version (`versionName` and `versionCode`) and the android version
  of the device.
- the component: an activity, a service, a preferences file, `SecureStorageHelper`,
  `PasswordHasher`, `PasswordAttemptPolicy`, `ScheduleAccessGate`…
- reproduction steps, as concrete as possible, and what an attacker gains.
- whether the device needs to be rooted, have usb debugging enabled or the app
  built as `debug` for the issue to reproduce.
- logs, stack traces or a proof of concept, if you have them.

### WHAT TO EXPECT

| stage | target |
|---|---|
| acknowledgement of the report | within 72 hours |
| initial assessment (accepted / not a vulnerability / needs more information) | within 7 days |
| fix in the supported version | depends on severity; critical issues take priority over any feature work |

this is a single-maintainer project without a paid bug bounty: there is no reward
beyond credit in the advisory and in `changelog.md`, and the timings above are
best effort, not a contractual sla.

---

## DISCLOSURE POLICY

- fixes are developed in private and published together with a github security
  advisory and a `#### SECURITY` entry in [`changelog.md`](./changelog.md).
- please keep the report private until the fix is published, or for 90 days from
  the acknowledgement, whichever comes first.
- reporters are credited by the name or handle they choose, unless they ask to
  stay anonymous.

---

## SCOPE

focusphone **has no `INTERNET` permission**: there is no backend, no account, no
analytics sdk and no crash reporter. so there is no remote attack surface, and the
threat model is entirely local. what the app does protect is documented in detail
in [`08-security.md`](./documentation/en/developers/08-security.md); in summary:

| protected against | mechanism |
|---|---|
| another app reading focusphone's data | app-private storage plus `AES-256/GCM` encryption backed by the android keystore |
| extracting the files from the device | sensitive values are useless without the keystore key, which never leaves the device |
| leakage through backup or device-to-device transfer | `allowBackup="false"` and `data_extraction_rules.xml` excluding every domain |
| brute forcing a detox or schedule password | `PBKDF2WithHmacSHA256`, 100 000 iterations, per-password salt, and one shared attempt policy: 5 failures then 15 minutes of refusal |
| shoulder surfing a password | `FLAG_SECURE` on the block screen and on every password dialog |

### IN SCOPE

- recovering a detox or schedule password, or bypassing a block without it.
- reading, tampering with or resetting encrypted preferences from outside the app
  (another app, an exported component, an intent, a backup).
- misuse of the keystore, of `SecureStorageHelper` or of `PasswordHasher`
  (weak parameters, iv reuse, a key regenerated over an existing alias).
- an exported component, intent or accessibility/notification service path that
  lets a third party act on the user's behalf.
- a leak of user habits — blocked apps, keywords, usage counters, custom labels —
  through logs, screenshots, backups or any world-readable file.

### OUT OF SCOPE

these are known, documented design decisions, not vulnerabilities:

- focusphone is **not a device administrator**: it does not prevent its own
  uninstall and does not prevent switching launchers. it is a reinforced
  commitment, not a cage.
- anything that requires a rooted device, an unlocked bootloader, usb debugging
  plus physical access, or a `debug` build.
- clearing the app's data or uninstalling it through android settings, which by
  design removes every block.
- reports produced only by an automated scanner, with no exploit path on a
  release build on a non-rooted device.
- missing hardening that has no attack path, such as the absence of certificate
  pinning in an app that makes no network requests.
- vulnerabilities in android itself, in oem software or in third-party apps.

---

## IF YOU ARE PATCHING SECURITY CODE

any change touching passwords, encryption or block screens must follow the rules
in [`08-security.md`](./documentation/en/developers/08-security.md), section
"rules when touching security" — among them: never delete protected configuration
in response to failed attempts, never treat an `Unavailable` keystore state as an
empty list, never run pbkdf2 on the main thread, and never regenerate the
keystore key over an existing alias. new edge cases need unit tests.

---

<div align="center">

<sub>focusphone — developed by <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
