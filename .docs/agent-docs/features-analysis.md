#  **FOCUSPHONE  |  AUDIT REPORT  |  FEATURES ANALYSIS**
> informacion sobre las funcionalidades de la aplicacion, se ha realizado un analisis.
---
##   **FEATURES ANALYSIS  |  SUMMARY**
- Remove unused dangerous permissions (READ_CONTACTS, PACKAGE_USAGE_STATS, NFC) and add privacy policy to avoid Play Store rejection/suspension.  
- Fix data integrity risks: atomic Room writes for toggle/add favourite, replace unscoped fire-and-forget migration coroutine, exclude plaintext detox password from cloud backup.  
- Resolve UI blockers: define missing @color resources (color_segmented_control_bg, etc.), enforce 48dp touch targets, add contentDescription to all functional icons/custom views.  
- Optimize performance: cache PackageManager query in AppRepositoryImpl, replace recursive BaseActivity theme traversal with theme attributes, throttle verifyInstalledApps in onResume.  
- Add release signing config, increment versionCode, expand ProGuard rules, and establish unit tests for use cases + repository logic.

---
##   **FEATURES ANALYSIS  |  FEATURE LIST**
###    **F1 — MINIMALIST TEXT-ONLY LAUNCHER (PRIMARY)**
  - Description: Replaces Android home screen with clock, calendar, and up to 10 text app labels.  
  - Dependencies: F-8, F-11, F-13  
  - Current issues: ISS-1, ISS-2, ISS-3
###    **F2 — FAVOURITE APPS (MAX 10) (PRIMARY)**  
  - Description: Searchable picker and Room-backed list with drag-to-reorder and custom labels.  
  - Dependencies: F-9, F-14, F-15  
  - Current issues: ISS-4, ISS-5, ISS-6
###    **F3 — CONTEXTUAL PROFILES (PRIMARY)**  
  - Description: Normal / Work / Detox blocking with hardcoded keyword list and password-protected detox timer.  
  - Dependencies: F-16, F-17  
  - Current issues: ISS-7, ISS-8, ISS-9
###    **F4 — CONSCIOUS PAUSE (PRIMARY)**  
  - Description: Breathing countdown animation before launching any app.  
  - Dependencies: F-18  
  - Current issues: ISS-10, ISS-11
###    **F5 — ALL APPS DRAWERS (PRIMARY)**  
  - Description: Searchable full app list with long-press rename/block and profile-aware launch.  
  - Dependencies: F-2, F-3, F-4  
  - Current issues: ISS-12, ISS-13
###    **F6 — THEMING SYSTEM (PRIMARY)**  
  - Description: 4 built-in themes + custom hex color with runtime recursive recoloring.  
  - Dependencies: F-19  
  - Current issues: ISS-14, ISS-15
###    **F7 — ONBOARDING FLOW (SECONDARY)**  
  - Description: 3-step ViewPager2 tutorial with launcher default setup and app selection redirect.  
  - Dependencies: F-8, F-2  
  - Current issues: ISS-16, ISS-17
###    **F8 — CLOCK / CALENDAR DISPLAY (SECONDARY)**  
  - Description: Live clock (5 types) and monthly calendar grid rebuilt on every resume.  
  - Dependencies: F-19  
  - Current issues: ISS-18, ISS-19
###    **F9 — APP SELECTION SCREEN (SECONDARY)**  
  - Description: Searchable list to pick favourite apps with instant add/remove indicator.  
  - Dependencies: F-2  
  - Current issues: ISS-12, ISS-20
###    **F10 — SETTINGS PANEL (SECONDARY)**  
  - Description: Theme, clock, font, language, conscious pause, tutorial, and default launcher shortcut.  
  - Dependencies: F-6, F-8, F-4  
  - Current issues: ISS-15, ISS-21, ISS-22
###    **F11 — DRAG-AND-DROP REORDING (SECONDARY)**  
  - Description: ItemTouchHelper-based reorder of home-screen favourites persisted to Room sortOrder.  
  - Dependencies: F-2  
  - Current issues: ISS-5
###    F12 — LONG-PRESS CONTEXT MENU (SECONDARY)  
  - Description: Popup menus on favourites (open/remove/info) and all-apps (rename/block).  
  - Dependencies: F-2, F-3  
  - Current issues: ISS-23
###    **F13 — DRAWER PUSH EFFECT (SECONDARY)**  
  - Description: Full-screen drawer that pushes main content with auto-hiding dot indicators.  
  - Dependencies: F-1  
  - Current issues: ISS-24
###    **F14 — CUSTOM APPS LABEL (SECONDARY)**  
  - Description: User-defined app names stored in SharedPreferences and merged into flows.  
  - Dependencies: F-2  
  - Current issues: ISS-25
###    **F15 — SHAREDPREFERENCES TO ROOM MIGRATION (SECONDARY)**  
  - Description: One-time migration of pipe-delimited favourites string into Room entities.  
  - Dependencies: F-2  
  - Current issues: ISS-26
###    **F16 — DETOX COUNTDOWN & BLOCK SCREEN (SECONDARY)**  
  - Description: Full-screen block page with live countdown and password verification.  
  - Dependencies: F-3  
  - Current issues: ISS-9, ISS-27
###    **F17 — WORK KEYWORD BLOCKING (SECONDARY)**  
  - Description: Hardcoded package-name keyword list blocked in Work profile.  
  - Dependencies: F-3  
  - Current issues: ISS-28
###    **F18 — BREATHING ANIMATION (SECONDARY)**  
  - Description: ScaleAnimator pulsing circle synchronized to inhale/exhale prompts.  
  - Dependencies: F-4  
  - Current issues: ISS-10
###    **F19 — BASEACTIVITY THEMING & INSETS (SECONDARY)**  
  - Description: Edge-to-edge, font scale, fade transitions, and custom theme application.  
  - Dependencies: none  
  - Current issues: ISS-14, ISS-29

---
##   **FEATURES ANALYSIS  |  UI/UX ISSUES (SELECTED)**
- ISS-1: Missing color resources cause runtime inflation failure — Define color_segmented_control_bg, color_segmented_text, color_switch_thumb, color_switch_track in colors.xml. (Priority: HIGH)  
- ISS-2: Home screen app list lacks bottom inset padding; last item may be hidden — Apply WindowInsets bottom padding to apps_container. (Priority: HIGH)  
- ISS-3: No landscape layouts; views overlap in landscape — Add res/layout-land/activity_home.xml. (Priority: HIGH)  
- ISS-4: Favourite app rows can shrink below 48dp — Enforce minHeight="48dp" on item_favourite_app.xml root. (Priority: HIGH)  
- ISS-5: Drag handle touch targets <48dp and sortOrder race — Ensure drag handle is 48dp and use Room @Transaction. (Priority: HIGH)  
- ISS-8: Plaintext detox password stored and backed up — Hash password with SHA-256 or AndroidX Security; exclude from backup. (Priority: HIGH)  
- ISS-14: Theme recursion causes frame drops — Apply custom colors via theme attributes or single root background. (Priority: HIGH)  
- ISS-15: Hardcoded Spanish strings — Move UI text to strings.xml and localize. (Priority: HIGH)

---
##   **FEATURES ANALYSIS  |  PERFORMANCE RECOMMENDATIONS**
- Replace recursive theme recoloring with theme attributes or root-level styling to reduce startup cost.  
- Initialize Room and SharedPreferences off the main thread or pre-warm during Application startup.  
- Cache installed apps in AppRepositoryImpl with a TTL (30s) and invalidate via package broadcasts.  
- Reuse calendar views or migrate to RecyclerView to avoid re-allocating ~42 views on every resume.  
- Throttle verifyInstalledApps to once per hour or use ACTION_PACKAGE_REMOVED to invalidate.

---
##   **FEATURES ANALYSIS  |  RELIABILITY RISKS & FIXES**
- Risk: data loss when adding/removing favourites due to non-atomic read-modify-write.  
  - Fix: Use Room @Transaction or single SQL statement (INSERT OR IGNORE + COUNT).  
- Risk: migration coroutine runs unscoped and may crash leaving partial state.  
  - Fix: Run migration in Application-scoped CoroutineScope with SupervisorJob and retry.  
- Risk: allowBackup=true may restore favourites pointing to missing packages.  
  - Fix: Exclude launcher DB from backup or add restore validation step.

---
##   **FEATURES ANALYSIS  |  TESTS & OBSERVABILITY**
- Unit tests: ToggleFavouriteUseCase, repository migration logic, ProfilePreferences password handling.  
- UI tests: onboarding flow, home drag-to-reorder, detox block, settings theme change.  
- Integration: Room migration tests using in-memory DB.  
- Perf: macrobenchmark cold start <500ms and measure frame drops for theme apply.  
- Observability: integrate Crashlytics or a local crash logger for ANR/crash collection.

---
##   **FEATURES ANALYSIS  |  PLAY STORE CHECKLIST**
- Remove unused permissions: READ_CONTACTS, PACKAGE_USAGE_STATS, NFC.  
- Add privacy policy page and link in Settings; declare QUERY_ALL_PACKAGES usage.  
- Add release signingConfig and increment versionCode.  
- Add screenshots for phone and 7-inch tablet in supported languages.  
- Expand ProGuard rules and verify release build on real device.

---
##   **FEATURES ANALYSIS  |  DEVICE MATRIX (TEST TARGETS)**
- Pixel 4a (small phone, Android 14/15)  
- Pixel 8 (standard phone, Android 15)  
- Samsung Galaxy S24 Ultra (large phone, Android 14)  
- Pixel Fold inner/outer displays (foldable)  
- Samsung Galaxy Tab S9 (tablet)  
- Low-end 720p emulator with 2GB RAM (Android 10)

---
##   **FEATURES ANALYSIS  |  CHECKLIST (ACTIONABLE)**
- Remove unused permissions from AndroidManifest.xml.  
- Add privacy policy and Play Console declarations.  
- Add release signingConfig and increment versionCode.  
- Define missing color resources and enforce 48dp touch targets.  
- Replace unscoped migration CoroutineScope with Application-scoped SupervisorJob and retry.  
- Wrap addFavourite/toggle operations in Room @Transaction.  
- Cache installed apps with TTL and broadcast invalidation.  
- Remove recursive theme traversal; apply theme attributes.  
- Add contentDescription to all functional icons and custom views.  
- Write unit tests for use cases and repository migrations.  
- Write UI tests for onboarding, app selection, detox flows, and settings.  
- Expand ProGuard rules and validate release build before upload.
