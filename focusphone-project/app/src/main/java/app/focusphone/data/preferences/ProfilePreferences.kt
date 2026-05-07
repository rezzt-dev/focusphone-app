package app.focusphone.data.preferences

import android.content.Context

/**
 * Synchronous preference manager for Contextual Profiles and Detox app blocking.
 */
object ProfilePreferences {

    private const val PREF_FILE     = "focusphone_profile"
    private const val KEY_PROFILE   = "current_profile"
    private const val KEY_DETOX_END = "detox_end_time"

    const val PROFILE_NORMAL = "normal"
    const val PROFILE_WORK   = "work"
    const val PROFILE_DETOX  = "detox"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun getProfile(context: Context): String =
        prefs(context).getString(KEY_PROFILE, PROFILE_NORMAL) ?: PROFILE_NORMAL

    fun setProfile(context: Context, profile: String) {
        prefs(context).edit().putString(KEY_PROFILE, profile).apply()
    }

    fun getDetoxEndTime(context: Context): Long =
        prefs(context).getLong(KEY_DETOX_END, 0L)

    fun setDetoxEndTime(context: Context, endTimeMs: Long) {
        prefs(context).edit().putLong(KEY_DETOX_END, endTimeMs).apply()
    }

    /**
     * Checks if the Detox profile is currently active and the timer hasn't expired.
     * Reverts automatically to normal if the timer has expired.
     */
    fun isDetoxActive(context: Context): Boolean {
        if (getProfile(context) != PROFILE_DETOX) return false
        val remaining = getDetoxEndTime(context) - System.currentTimeMillis()
        if (remaining <= 0) {
            setProfile(context, PROFILE_NORMAL)
            return false
        }
        return true
    }

    private const val KEY_BLOCKED_APPS = "blocked_apps"

    /**
     * Standard list of distracting keywords found in common distracting applications.
     */
    val DISTRACTING_KEYWORDS = listOf(
        "instagram", "facebook", "twitter", "tiktok", "youtube", "snapchat",
        "netflix", "pinterest", "twitch", "discord", "reddit", "linkedin",
        "clash", "candycrush", "pubg", "fortnite"
    )

    fun getBlockedApps(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setAppBlockedState(context: Context, packageName: String, blocked: Boolean) {
        val current = getBlockedApps(context).toMutableSet()
        if (blocked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs(context).edit().putStringSet(KEY_BLOCKED_APPS, current).apply()
    }

    /**
     * Determines whether an application is blocked under the current profile.
     */
    fun isAppBlocked(context: Context, packageName: String): Boolean {
        val profile = getProfile(context)
        if (profile == PROFILE_NORMAL) return false

        // 1. If Detox is active, block ALL applications except FocusPhone itself!
        if (isDetoxActive(context)) {
            val ownPackage = context.packageName
            return packageName != ownPackage
        }

        // 2. If Work profile is active, block only manually blocked or default distracting ones
        if (profile == PROFILE_WORK) {
            // Check if the app is manually blocked by the user
            if (getBlockedApps(context).contains(packageName)) return true

            // Fallback to standard distracting keywords
            val pkgLower = packageName.lowercase()
            return DISTRACTING_KEYWORDS.any { pkgLower.contains(it) }
        }

        return false
    }
}
