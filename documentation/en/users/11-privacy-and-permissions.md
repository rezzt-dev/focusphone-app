<div align="center">

# PRIVACY AND PERMISSIONS

<sub><a href="../README.md">index</a> · <a href="../../es/usuarios/11-privacidad-y-permisos.md">castellano</a> · previous: <a href="./10-settings-reference.md">settings reference</a> · next: <a href="./12-faq.md">faq</a></sub>

</div>

---

## SUMMARY IN FIVE POINTS

<table>
<tr><td width="4%">1</td><td><strong>focusphone does not connect to the internet.</strong> there are no accounts, no sync, no servers and no in-app online updates.</td></tr>
<tr><td>2</td><td><strong>there are no ads and no telemetry.</strong> no usage data is collected, anonymous or otherwise.</td></tr>
<tr><td>3</td><td><strong>everything stays on the device.</strong> your favourites, custom names, folders, rules and passwords never leave your phone.</td></tr>
<tr><td>4</td><td><strong>sensitive data is encrypted.</strong> the data that reveals how you use your phone is stored encrypted with a key that never leaves android's secure hardware.</td></tr>
<tr><td>5</td><td><strong>there is no backup.</strong> neither to the cloud nor through phone-to-phone transfer: focusphone's data is excluded on purpose.</td></tr>
</table>

---

## PERMISSIONS THE APP DECLARES

focusphone declares three permissions in total, and requests two optional services.

<table>
<tr>
<th align="left" width="30%">permission</th>
<th align="left" width="20%">when it is granted</th>
<th align="left" width="50%">what it is used for</th>
</tr>
<tr>
<td><strong>see all apps</strong><br><code>QUERY_ALL_PACKAGES</code></td>
<td>automatically, at install time</td>
<td>listing the installed apps in the drawer and the favourites selector. it is essential for any launcher: without it, there is nothing to show.</td>
</tr>
<tr>
<td><strong>request uninstalls</strong><br><code>REQUEST_DELETE_PACKAGES</code></td>
<td>automatically, at install time</td>
<td>opening android's uninstall dialog when you choose <em>uninstall</em>. focusphone <strong>never uninstalls anything by itself</strong>: the system always confirms.</td>
</tr>
<tr>
<td><strong>expand the status bar</strong><br><code>EXPAND_STATUS_BAR</code></td>
<td>automatically, at install time</td>
<td>the swipe-down gesture that opens the system notification shade.</td>
</tr>
</table>

### OPTIONAL SERVICES

<table>
<tr>
<th align="left" width="30%">service</th>
<th align="left" width="20%">when it is asked for</th>
<th align="left" width="50%">what it does and does not do</th>
</tr>
<tr>
<td><strong>accessibility service</strong></td>
<td>when starting a detox, or from settings → system</td>
<td><strong>does</strong>: detects which app comes to the foreground so it can be blocked if needed, counts usage time for daily limits, and expands the notification shade.<br><strong>does not</strong>: read screen content, type text, press buttons for you, or send anything anywhere.</td>
</tr>
<tr>
<td><strong>notification access</strong></td>
<td>when enabling the notification panel</td>
<td><strong>does</strong>: show your notifications in the side drawer and let you dismiss them.<br><strong>does not</strong>: store the content on disk, index it, or send it anywhere.</td>
</tr>
</table>

> both services are **optional**. focusphone works without either; you only lose blocking
> from outside the app, daily usage limits and the notification panel.

---

## WHAT DATA IT STORES AND WHERE

all data lives in the app's private storage, which no other app can reach.

<table>
<tr>
<th align="left" width="35%">data</th>
<th align="left" width="30%">how it is stored</th>
<th align="left" width="35%">encrypted</th>
</tr>
<tr><td>favourite apps and their order</td><td>local database</td><td>no, it is not sensitive by itself</td></tr>
<tr><td>theme, typeface, size, alignment</td><td>local preferences</td><td>no</td></tr>
<tr><td>clock, widget and layout settings</td><td>local preferences</td><td>no</td></tr>
<tr><td>wallpaper settings</td><td>local preferences</td><td>no</td></tr>
<tr><td>wallpaper image</td><td>a copy in the app's private storage</td><td>no, but unreachable by other apps</td></tr>
<tr><td><strong>custom app names</strong></td><td>local preferences</td><td><strong>yes</strong>, aes-256/gcm</td></tr>
<tr><td><strong>drawer folders</strong></td><td>local preferences</td><td><strong>yes</strong>, aes-256/gcm</td></tr>
<tr><td><strong>active profile, detox timer and blocked apps</strong></td><td>local preferences</td><td><strong>yes</strong>, aes-256/gcm</td></tr>
<tr><td><strong>work mode keywords</strong></td><td>local preferences</td><td><strong>yes</strong>, aes-256/gcm</td></tr>
<tr><td><strong>schedule rules and usage counters</strong></td><td>local preferences</td><td><strong>yes</strong>, aes-256/gcm</td></tr>
<tr><td><strong>detox and schedule passwords</strong></td><td>local preferences</td><td><strong>the password is never stored</strong>: only a cryptographic digest with a random salt</td></tr>
<tr><td>notifications shown in the panel</td><td>ram only</td><td>never written to disk</td></tr>
</table>

<details>
<summary><strong>what "encrypted with aes-256/gcm" means</strong></summary>

<br>

the data is stored as unreadable text. the key needed to read it back is generated and held
by the **android keystore**, a hardware-backed component on most modern devices: the key
never leaves it, not even focusphone can read it — it can only ask it to encrypt and
decrypt.

in practice: if someone extracts the files from your phone, they get unreadable data. and
since the key cannot be copied to another device, a backup restored on another phone would
be useless too.

</details>

<details>
<summary><strong>why passwords are not stored</strong></summary>

<br>

what is stored is not your password, but the result of running it through **pbkdf2-hmac-sha256
with 100,000 iterations and a random 128-bit salt**. the original password cannot be
recovered from that result, and the salt means two people with the same password get
different digests.

the 100,000 iterations make each check deliberately slow, which makes a brute-force attack
impractical.

</details>

---

## BACKUPS

focusphone **excludes itself from every backup mechanism**:

| mechanism | status | why |
|---|---|---|
| android cloud backup | disabled | keeps your favourites, themes and custom names from leaving the device |
| `adb backup` | disabled | same reason |
| device-to-device transfer | excluded | since android 12 this transfer is not covered by the setting above, so it is excluded explicitly |

**practical consequence**: when switching phones or reinstalling the app, you will have to
pick your favourites and settings again. that is the price of your data not travelling
anywhere.

---

## WHAT FOCUSPHONE **CANNOT** DO

| | |
|---|---|
| it cannot stop you from uninstalling it | it is not a device administrator; a detox session does not survive an uninstall |
| it cannot stop you from changing launcher | you can always go back to your previous launcher from android settings |
| it cannot lock the system settings | android settings always stay reachable |
| it cannot monitor someone else's phone | it sends no reports anywhere and has no admin mode |
| it cannot read your messages or content | not even with the accessibility service on: it only checks which app is in the foreground |

focusphone's locks are **a commitment to yourself**, reinforced with passwords and cooldowns
so they cannot be undone on impulse, but never a cage you cannot get out of.

---

## ADDITIONAL PROTECTIONS

| protection | detail |
|---|---|
| screenshots blocked | the detox block screen and the password dialogs cannot be captured or recorded |
| hidden rules | the schedules screen shows nothing, not even the list of apps with rules, until the correct password is entered |
| no wiping on failed attempts | failing the password never deletes the configuration: it only starts a 15-minute cooldown |
| colour validation | custom theme colours are validated before use, and an invalid value is replaced by a safe one |

---

<div align="center">

<sub><a href="./10-settings-reference.md">← settings reference</a> · <a href="../README.md">index</a> · <a href="./12-faq.md">faq →</a></sub>

</div>
