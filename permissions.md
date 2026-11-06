<div align="center">

# PERMISSIONS AND EXPORTED COMPONENTS

<sub>english · <a href="./permissions.es.md">castellano</a> · <a href="./privacy.md">privacy</a> · <a href="./security.md">security</a> · <a href="./readme.md">readme</a></sub>

</div>

---

technical justification of every permission and exported component in
`focusphone-project/app/src/main/AndroidManifest.xml`, checked against the merged manifest
of the `1.4.0-beta` release build (`aapt2 dump permissions`). it is meant for store review
(google play) and for security audits; the user-facing summary is in
[`privacy.md`](./privacy.md) and in
[`11-privacy-and-permissions.md`](./documentation/en/users/11-privacy-and-permissions.md).

**the app does not request `INTERNET` or any other network permission**, nor any runtime
("dangerous") permission: there is no permission dialog at first launch.

---

## 1. DECLARED PERMISSIONS

| permission | protection level | used by | why it is needed | without it |
|---|---|---|---|---|
| `android.permission.QUERY_ALL_PACKAGES` | normal (granted at install) | `AppRepositoryImpl` and `WidgetLaunchActivity`, through `PackageManager.queryIntentActivities` for `ACTION_MAIN` / `CATEGORY_LAUNCHER` | a launcher must list every launchable app; from android 11 (api 30) package visibility hides them otherwise | the drawer, the favourites selector and the widget picker would only see a handful of system packages |
| `android.permission.REQUEST_DELETE_PACKAGES` | normal (granted at install) | `LauncherUtils.requestUninstall`, which starts `Intent.ACTION_DELETE` | apps targeting api 28+ need it to open android's uninstall dialog; android always asks the user to confirm and focusphone never uninstalls anything itself | the "uninstall" entry of an app's options could not open the system dialog |
| `android.permission.EXPAND_STATUS_BAR` | normal (granted at install) | `HomeActivity.expandNotificationsPanel`, fallback path only | the swipe-down gesture prefers `AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS`; when the accessibility service is off, it falls back to `StatusBarManager.expandNotificationsPanel`, which needs this permission and only works on older android versions (the call is blocked from api 31) | the gesture would do nothing when the accessibility service is off |
| `app.focusphone.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | signature | added by `androidx.core` to the merged manifest | lets `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` keep dynamically registered receivers private on android versions before 13 | not applicable: it is defined and held only by focusphone itself |

---

## 2. OPTIONAL SERVICES THE USER ENABLES

these are not permissions focusphone holds: the service is bound by android, and only after
the user enables it in android settings. both can be disabled at any time.

### 2.1 ACCESSIBILITY SERVICE — `FocusAccessibilityService`

| | |
|---|---|
| guarded by | `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` (only the system can bind it) |
| configuration | `res/xml/accessibility_service_config.xml`: `accessibilityEventTypes="typeWindowStateChanged\|typeWindowsChanged"`, `canRetrieveWindowContent="true"`, `accessibilityFlags="flagRetrieveInteractiveWindows"` |
| what it reads | the package name of the active window (`AccessibilityWindowInfo.root.packageName`), falling back to the event's package name. `canRetrieveWindowContent` and `flagRetrieveInteractiveWindows` are required to obtain that window list; no node text is read |
| what it does | shows `DetoxBlockedActivity` / `ScheduleBlockedActivity` on top of a blocked app opened from outside focusphone (notifications, recents, suggested shortcuts, assistants); accumulates foreground seconds per app for daily limits, persisted encrypted by `AppSchedulePreferences`; performs `GLOBAL_ACTION_NOTIFICATIONS` for the swipe-down gesture |
| what it never does | read or store screen text, take screenshots, perform clicks or gestures in other apps, type, or send data anywhere (the app has no network access) |
| when it is requested | when starting a detox session, from settings → system, and from the schedules screen; never enabled automatically |
| without it | profile blocks and schedules only apply to apps launched from focusphone itself (home, drawer, widgets); daily limits are not measured; the swipe-down gesture falls back to `EXPAND_STATUS_BAR` |

### 2.2 NOTIFICATION LISTENER — `FocusNotificationService`

| | |
|---|---|
| guarded by | `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` |
| what it reads | the active notifications, to list them in the side panel; the active media sessions (`MediaSessionManager.getActiveSessions`, which requires an enabled listener) for title, artist, artwork and playback state in the music widgets |
| storage | notification content is kept in memory only and cleared in `onListenerDisconnected` / `onDestroy`; media metadata is read on demand and not stored |
| when it is requested | from settings → notifications, when the user turns on the notification panel; never enabled automatically |
| without it | no notification panel; music widgets show a "no access" state |

---

## 3. EXPORTED COMPONENTS

| component | exported because | protection |
|---|---|---|
| `ui.home.HomeActivity` | it is the `MAIN` / `HOME` / `LAUNCHER` entry point | `singleTask`, `stateNotNeeded`; the only extra it reads from an intent is the boolean `EXTRA_EDIT_LAYOUT`, which opens edit mode and grants nothing |
| 20 `AppWidgetProvider`s in `ui.widget.library` | android requires providers to be exported so any launcher can host them (`APPWIDGET_UPDATE`) | providers hold no logic; an update only redraws from focusphone's own state, and every tap goes through `WidgetLaunchActivity` (not exported), which applies profile, schedule and conscious pause |
| `ui.widget.library.WidgetSystemReceiver` | to receive system broadcasts that change what widgets show | only listens to protected system broadcasts (`TIME_SET`, `TIMEZONE_CHANGED`, `DATE_CHANGED`, `LOCALE_CHANGED`, `NEXT_ALARM_CLOCK_CHANGED`, `MY_PACKAGE_REPLACED`) and only triggers a redraw |
| `service.FocusAccessibilityService` | required by android for accessibility services | `BIND_ACCESSIBILITY_SERVICE` |
| `service.FocusNotificationService` | required by android for notification listeners | `BIND_NOTIFICATION_LISTENER_SERVICE` |

every other activity and receiver, including `WidgetActionReceiver`, `WidgetLaunchActivity`,
`WorldClockConfigActivity` and `IntentionConfigActivity`, is `android:exported="false"`.

---

## 4. BACKUP AND DATA EXTRACTION

`android:allowBackup="false"` plus `android:dataExtractionRules="@xml/data_extraction_rules"`,
which excludes the `root`, `file`, `database`, `sharedpref` and `external` domains from both
`cloud-backup` and `device-transfer`. see [`privacy.md`](./privacy.md#6-backups-and-changing-devices).

---

## 5. GOOGLE PLAY NOTES

focusphone is distributed as an apk on github releases and is not on google play. if it is
submitted, these declarations would be needed:

| item | play policy | status |
|---|---|---|
| `QUERY_ALL_PACKAGES` | package (app) visibility policy: broad visibility must be declared; launchers are an accepted use case | declaration form to be filed in play console |
| accessibility service | accessibility api policy: an app that is not an accessibility tool must show a prominent in-app disclosure and obtain affirmative consent before enabling it, and declare the use in play console | **TODO(owner)**: the current in-app text (`detox_accessibility_desc`, `schedule_accessibility_required_desc`) is a short explanation, not a full prominent disclosure with explicit consent |
| data safety form | declare "no data collected, no data shared" | consistent with [`privacy.md`](./privacy.md) |
| privacy policy url | required for sensitive permissions | **TODO(owner)**: publish a stable url for [`privacy.md`](./privacy.md) |

---

<div align="center">

<sub>focusphone — developed by <a href="https://rezzt.dev"><b>rezzt.dev</b></a></sub>

</div>
