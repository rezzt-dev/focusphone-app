<div align="center">

# FREQUENTLY ASKED QUESTIONS

<sub><a href="../README.md">index</a> · <a href="../../es/usuarios/12-preguntas-frecuentes.md">castellano</a> · previous: <a href="./11-privacy-and-permissions.md">privacy and permissions</a></sub>

</div>

---

## GENERAL USE

<details>
<summary><strong>does focusphone uninstall or hide my apps?</strong></summary>

<br>

no. focusphone only decides **what it shows** on the home screen. all your apps stay
installed and reachable from the all apps drawer. if you go back to your previous launcher,
you will find everything exactly as it was.

</details>

<details>
<summary><strong>why only 10 favourite apps?</strong></summary>

<br>

because that is the cap that keeps the home screen a short list you can read at a glance. if
thirty fitted, it would be a grid of options again and the app would lose its point. the rest
of your apps are two gestures away in the drawer.

</details>

<details>
<summary><strong>can i go back to my previous launcher?</strong></summary>

<br>

yes, whenever you want: android settings → apps → default apps → home app, and pick the one
you used before. you lose nothing.

</details>

<details>
<summary><strong>does focusphone need internet?</strong></summary>

<br>

no, at no point. the app is fully offline: no accounts, no sync, no servers.

</details>

<details>
<summary><strong>are there ads or in-app purchases?</strong></summary>

<br>

neither.

</details>

---

## HOME SCREEN AND GESTURES

<details>
<summary><strong>the swipe gestures do not respond</strong></summary>

<br>

check the following, in this order:

1. that you are swiping on the home screen, not inside an already open drawer: with a drawer
   open, the gesture will not open the opposite one
2. that the gesture is long and fast enough: a very slow drag does not count as a swipe
3. for the notification panel, that it is enabled in **settings → notifications** and that
   you granted notification access
4. for the swipe-down gesture, that it is enabled and, preferably, that the accessibility
   service is active

</details>

<details>
<summary><strong>the swipe-down gesture does not open the system shade</strong></summary>

<br>

on android 12 and later, the fallback method focusphone uses without the accessibility
service is blocked by the system itself. the fix is to enable the accessibility service in
**settings → system → system blocking**: with it, the gesture uses the official route and
always works.

</details>

<details>
<summary><strong>the calendar still highlights yesterday</strong></summary>

<br>

it should not: focusphone rebuilds the calendar when the day changes, even if the home screen
has been open for days. if you see it, open and close the side menu to force a refresh, and
report it as an issue.

</details>

<details>
<summary><strong>can i have several home screens?</strong></summary>

<br>

no, and that is deliberate. focusphone has a single grid page plus two side drawers. what
you can do is arrange that page as you like: up to 6 columns by 12 rows, with the clock, your
favourites and any number of widgets (see
[edit mode](./03-home-screen-and-gestures.md#edit-mode)).

</details>

---

## BLOCKS, PROFILES AND SCHEDULES

<details>
<summary><strong>i forgot the detox password</strong></summary>

<br>

there is no way to recover it, but **you are not trapped**: every detox session has a timer
and ends on its own when it expires, even if you restart the phone. you just have to wait.
once the session ends, you can start a new one with a different password.

</details>

<details>
<summary><strong>i forgot the schedules password</strong></summary>

<br>

the schedules password protects the configuration screen. if you forget it, the rules keep
applying and you will not be able to edit them from inside the app. as a last resort,
clearing focusphone's data from android settings removes the whole configuration, including
the rules and the password — but also your favourites and settings.

</details>

<details>
<summary><strong>i failed the password several times and now even the correct one is refused</strong></summary>

<br>

that is the expected behaviour: after **5 failed attempts** there is a **15-minute** cooldown
in which every attempt is refused, including the correct one. without that wait, trying
passwords would cost nothing. wait and try again.

nothing has been deleted: not the password, not the rules, not the session.

</details>

<details>
<summary><strong>a blocked app still opens from a notification</strong></summary>

<br>

that happens when the **accessibility service** is not active. without it, focusphone can only
block apps launched from the app itself. enable it in **settings → system → system blocking**
and the app will be blocked no matter where it is opened from.

</details>

<details>
<summary><strong>the daily minute limit is not counting down</strong></summary>

<br>

daily limits need the **accessibility service**: it is the only component that can measure
how long an app stays in the foreground. without it, the time window works but the minute
counter does not advance.

</details>

<details>
<summary><strong>why does the schedule block screen have no unlock button?</strong></summary>

<br>

because a schedule is a permanent configuration, not a session. offering an unlock on the
block screen itself would place the way out at the moment of greatest temptation. to change a
rule you have to go to **settings → focus → app schedules** and enter the password.

</details>

<details>
<summary><strong>the work profile blocks an app i did not want blocked</strong></summary>

<br>

almost always because of a **keyword** that is too generic. review
**settings → focus → work mode keywords**, remove the word causing the match and, if you
still want a specific app blocked, mark it by hand from the all apps drawer.

</details>

<details>
<summary><strong>can focusphone be used to control someone else's phone?</strong></summary>

<br>

it is not built for that. there is no admin mode, no remote reports and no uninstall
protection. focusphone is a tool for setting limits on yourself.

</details>

---

## APPEARANCE

<details>
<summary><strong>i set a custom theme and the text is barely readable</strong></summary>

<br>

focusphone automatically adjusts the text colour until it reaches a minimum contrast against
the background, so the interface never becomes fully unreadable. if you are still not happy,
pick colours further apart: a dark background with light text, or the other way round.

</details>

<details>
<summary><strong>i set a wallpaper and i cannot read the text well</strong></summary>

<br>

raise the **text legibility** slider in **settings → appearance → wallpaper**, and turn on
**blur**. both settings apply instantly and the preview shows the result without leaving the
screen.

</details>

<details>
<summary><strong>the wallpaper disappeared after a restart</strong></summary>

<br>

it should not: focusphone copies the image into its private storage precisely so it survives
restarts. if it happens, pick it again and report it as an issue.

</details>

<details>
<summary><strong>can i change the clock's 12 or 24 hour format?</strong></summary>

<br>

yes, but from android settings: focusphone follows the system format so there are not two
places configuring the same thing.

</details>

---

## DATA AND UPDATES

<details>
<summary><strong>are my settings kept when i switch phones?</strong></summary>

<br>

no. focusphone is deliberately excluded from cloud backup and from device-to-device transfer.
you will have to set it up again on the new phone. that is the price of your data not
travelling anywhere.

</details>

<details>
<summary><strong>do i lose my configuration when i update the app?</strong></summary>

<br>

no. a normal update keeps all data. it is only lost if you uninstall the app or clear its
data from android settings.

</details>

<details>
<summary><strong>how does focusphone update?</strong></summary>

<br>

by downloading the new version's apk from the repository's releases tab and installing it
over the previous one. the app does not check for updates by itself, because it does not
connect to the internet. an update only installs over the previous version when both are
signed with the same key; `v1.0.0-beta` was not, so coming from it you have to uninstall
first (see [installation](./02-installation-and-first-steps.md)).

</details>

---

## PROBLEMS AND REPORTS

<details>
<summary><strong>the app behaves oddly after an update</strong></summary>

<br>

try, in this order:

1. close focusphone completely from recents and reopen it
2. restart the phone
3. check that the optional permissions are still granted: some manufacturers disable
   accessibility services after an update

</details>

<details>
<summary><strong>where do i report a bug or suggest a feature?</strong></summary>

<br>

in the repository's **issues** tab on github. for a bug, it helps a lot to include:

- the focusphone version (settings → about → version)
- the phone model and the android version
- the exact steps to reproduce it
- whether the accessibility service was active

</details>

<details>
<summary><strong>i want to understand how it works inside</strong></summary>

<br>

all the technical documentation is in the developer section, starting with
**[architecture](../developers/01-architecture.md)**.

</details>

---

<div align="center">

<sub><a href="./11-privacy-and-permissions.md">← privacy and permissions</a> · <a href="../README.md">index</a></sub>

</div>
