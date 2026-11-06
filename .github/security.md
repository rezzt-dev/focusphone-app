<div align="center">

# SECURITY POLICY

<sub>english · <a href="./security.es.md">castellano</a> · <a href="../readme.md">readme</a> · <a href="../documentation/en/developers/08-security.md">full security documentation</a> · <a href="../repo-docs/permissions.md">permissions</a></sub>

<br>

<img src="https://img.shields.io/badge/100%25-offline-4c1?style=flat-square" alt="100% offline" />
<img src="https://img.shields.io/badge/no-trackers-4c1?style=flat-square" alt="no trackers" />
<img src="https://img.shields.io/badge/reports-private-000000?style=flat-square" alt="private reports" />

</div>

---

## SUPPORTED VERSIONS

focusphone is in beta and there is a single supported line: the latest published
version. there are no long-term support branches and no backports.

| version (settings → about) | release | supported |
|---|---|---|
| `1.4.0-beta` | [`v1.4.0`](https://github.com/rezzt-dev/focusphone-app/releases) | yes |
| `1.0.6` built from the 1.4 code | [`v1.4-beta`](https://github.com/rezzt-dev/focusphone-app/releases/tag/v1.4-beta) | no — same code as `1.4.0-beta`, update to it |
| `1.0.6` built from the 1.0 code | [`v1.0.0-beta`](https://github.com/rezzt-dev/focusphone-app/releases/tag/v1.0.0-beta) | no — signed with a different key, see below |
| any alpha | — | no |

before reporting, make sure the issue still reproduces on the latest release.
`versionName` lives in `focusphone-project/app/build.gradle.kts` and the app shows it in
**settings → about**.

---

## VERIFYING AN APK

every release from `v1.4-beta` on is signed with the same certificate. check it before
installing:

```bash
sha256sum -c SHA256SUMS                     # the file attached to the release
apksigner verify --print-certs focusphone-*.apk
```

| | |
|---|---|
| signature scheme | apk signature scheme v2 |
| certificate sha-256 | `0f:f1:f8:3c:32:e0:5d:c5:4b:c2:2b:a2:8a:13:df:4c:5c:37:ec:79:c4:1d:b3:e1:4a:d7:20:e7:c8:a0:76:2f` |

the `v1.0.0-beta` apk was signed with a different key that is no longer available. android
refuses to update an app signed with another key, so **if you installed `v1.0.0-beta`,
uninstall it first** (this deletes its settings, since focusphone keeps no backups) and then
install the current release. any apk that claims to be focusphone and is signed with a
certificate other than the one above is not an official build.

---

## REPORTING A VULNERABILITY

**do not open a public issue for a security problem.** use one of these private channels:

| channel | when |
|---|---|
| email to `contacto@rezzt.dev` with the subject `[focusphone security]` | always available |
| [github private vulnerability reporting](https://github.com/rezzt-dev/focusphone-app/security/advisories/new) | when the form is enabled on the repository; the report stays private until a fix is published |

<!-- TODO(owner): enable private vulnerability reporting in the repository settings; until then the link above shows no form. -->

### WHAT TO INCLUDE

- the affected version (`versionName` and `versionCode`), how it was installed (which
  release asset) and the android version of the device.
- the component: an activity, a service, a preferences file, `SecureStorageHelper`,
  `PasswordHasher`, `PasswordAttemptPolicy`, `ScheduleAccessGate`…
- reproduction steps, as concrete as possible, and what an attacker gains.
- whether the device needs to be rooted, have usb debugging enabled or the app built as
  `debug` for the issue to reproduce.
- logs, stack traces or a proof of concept, if you have them.

### WHAT TO EXPECT

| stage | target |
|---|---|
| acknowledgement of the report | within 72 hours |
| initial assessment (accepted / not a vulnerability / needs more information) | within 7 days |
| fix in the supported version | depends on severity; critical issues take priority over any feature work |

this is a single-maintainer project **without a bug bounty**: there is no reward beyond
credit in the advisory and in `changelog.md`, and the timings above are best effort, not a
contractual sla.

---

## COORDINATED DISCLOSURE

- fixes are developed in private and published together with a github security advisory
  and a `#### SECURITY` entry in [`changelog.md`](../repo-docs/changelog.md).
- please keep the report private until the fix is published, or for 90 days from the
  acknowledgement, whichever comes first.
- reporters are credited by the name or handle they choose, unless they ask to stay
  anonymous.
- a report is handled as described in [`privacy.md`](../repo-docs/privacy.md#8-your-rights): the data
  you send is used only to process it.

---

## SCOPE

focusphone **has no `INTERNET` permission**: there is no backend, no account, no analytics
sdk and no crash reporter. so there is no remote attack surface, and the threat model is
entirely local. what the app does protect is documented in detail in
[`08-security.md`](../documentation/en/developers/08-security.md); in summary:

| protected against | mechanism |
|---|---|
| another app reading focusphone's data | app-private storage plus `AES-256/GCM` encryption backed by the android keystore |
| extracting the files from the device | sensitive values are useless without the keystore key, which never leaves the device |
| leakage through backup or device-to-device transfer | `allowBackup="false"` and `data_extraction_rules.xml` excluding every domain |
| brute forcing a detox or schedule password | `PBKDF2WithHmacSHA256`, 100 000 iterations, per-password salt, and one shared attempt policy: 5 failures then 15 minutes of refusal |
| shoulder surfing a password | `FLAG_SECURE` on the block screen and on every password dialog |
| a tampered apk | the release signing certificate above |

### IN SCOPE

- recovering a detox or schedule password, or bypassing a block without it.
- reading, tampering with or resetting encrypted preferences from outside the app (another
  app, an exported component, an intent, a backup).
- misuse of the keystore, of `SecureStorageHelper` or of `PasswordHasher` (weak
  parameters, iv reuse, a key regenerated over an existing alias).
- an exported component, intent, widget or accessibility/notification service path that
  lets a third party act on the user's behalf (see [`permissions.md`](../repo-docs/permissions.md#3-exported-components)).
- a leak of user habits — blocked apps, keywords, usage counters, custom labels, the
  intention text — through logs, screenshots, backups or any world-readable file.

### OUT OF SCOPE

these are known, documented design decisions, not vulnerabilities
([`disclaimer.md`](../repo-docs/disclaimer.md)):

- focusphone is **not a device administrator**: it does not prevent its own uninstall and
  does not prevent switching launchers. it is a reinforced commitment, not a cage.
- anything that requires a rooted device, an unlocked bootloader, usb debugging plus
  physical access, or a `debug` build.
- clearing the app's data or uninstalling it through android settings, which by design
  removes every block.
- what a third-party launcher does with the content of a focusphone widget it hosts.
- reports produced only by an automated scanner, with no exploit path on a release build
  on a non-rooted device.
- missing hardening that has no attack path, such as the absence of certificate pinning in
  an app that makes no network requests.
- vulnerabilities in android itself, in oem software or in third-party apps.

---

## IF YOU ARE PATCHING SECURITY CODE

any change touching passwords, encryption or block screens must follow the rules in
[`08-security.md`](../documentation/en/developers/08-security.md), section "rules when
touching security" — among them: never delete protected configuration in response to
failed attempts, never treat an `Unavailable` keystore state as an empty list, never run
pbkdf2 on the main thread, and never regenerate the keystore key over an existing alias.
new edge cases need unit tests, and the pull request needs the maintainer's review
([`.github/CODEOWNERS`](./CODEOWNERS)).

---

<div align="center">

<sub>focusphone — developed by <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
