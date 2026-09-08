package app.focusphone.data.preferences

import android.content.Context

/**
 * Synchronous SharedPreferences storage for wallpaper backgrounds and blur settings.
 *
 * Supports a global enable/disable toggle, a separate image for each wallpaper-aware
 * surface (home screen, notification drawer and settings screens), a shared wallpaper mode,
 * and a blur toggle with intensity slider.
 */
object WallpaperPreferences {

    private const val PREF_FILE = "focusphone_wallpaper"

    private const val KEY_HOME_WALLPAPER_URI = "home_wallpaper_uri"
    private const val KEY_NOTIFICATIONS_WALLPAPER_URI = "notifications_wallpaper_uri"
    private const val KEY_SETTINGS_WALLPAPER_URI = "settings_wallpaper_uri"
    private const val KEY_SHARED_WALLPAPER_URI = "shared_wallpaper_uri"
    private const val KEY_USE_SHARED_WALLPAPER = "use_shared_wallpaper"
    private const val KEY_BLUR_ENABLED = "wallpaper_blur_enabled"
    private const val KEY_BLUR_INTENSITY = "wallpaper_blur_intensity"
    private const val KEY_WALLPAPER_ENABLED = "wallpaper_enabled"

    private const val DEFAULT_BLUR_INTENSITY = 50

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun isUseSharedWallpaper(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_SHARED_WALLPAPER, false)

    fun setUseSharedWallpaper(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_USE_SHARED_WALLPAPER, enabled).apply()

    fun getSharedWallpaperUri(context: Context): String? =
        prefs(context).getString(KEY_SHARED_WALLPAPER_URI, null)?.takeIf { it.isNotBlank() }

    fun setSharedWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit().putString(KEY_SHARED_WALLPAPER_URI, uri).apply()

    fun getHomeWallpaperUri(context: Context): String? =
        resolveEffectiveUri(context, KEY_HOME_WALLPAPER_URI)

    fun getRawHomeWallpaperUri(context: Context): String? =
        prefs(context).getString(KEY_HOME_WALLPAPER_URI, null)?.takeIf { it.isNotBlank() }

    fun setHomeWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit().putString(KEY_HOME_WALLPAPER_URI, uri).apply()

    fun getNotificationsWallpaperUri(context: Context): String? =
        resolveEffectiveUri(context, KEY_NOTIFICATIONS_WALLPAPER_URI)

    fun getRawNotificationsWallpaperUri(context: Context): String? =
        prefs(context).getString(KEY_NOTIFICATIONS_WALLPAPER_URI, null)?.takeIf { it.isNotBlank() }

    fun setNotificationsWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit().putString(KEY_NOTIFICATIONS_WALLPAPER_URI, uri).apply()

    fun getSettingsWallpaperUri(context: Context): String? =
        resolveEffectiveUri(context, KEY_SETTINGS_WALLPAPER_URI)

    fun getRawSettingsWallpaperUri(context: Context): String? =
        prefs(context).getString(KEY_SETTINGS_WALLPAPER_URI, null)?.takeIf { it.isNotBlank() }

    fun setSettingsWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit().putString(KEY_SETTINGS_WALLPAPER_URI, uri).apply()

    private fun resolveEffectiveUri(context: Context, ownKey: String): String? {
        if (isUseSharedWallpaper(context)) {
            return getSharedWallpaperUri(context)
        }
        return prefs(context).getString(ownKey, null)?.takeIf { it.isNotBlank() }
    }

    fun isBlurEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLUR_ENABLED, false)

    fun setBlurEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLUR_ENABLED, enabled).apply()

    fun getBlurIntensity(context: Context): Int =
        prefs(context).getInt(KEY_BLUR_INTENSITY, DEFAULT_BLUR_INTENSITY)
            .coerceIn(0, 100)

    fun setBlurIntensity(context: Context, intensity: Int) =
        prefs(context).edit().putInt(KEY_BLUR_INTENSITY, intensity.coerceIn(0, 100)).apply()

    fun isWallpaperEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WALLPAPER_ENABLED, true)

    fun setWallpaperEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_WALLPAPER_ENABLED, enabled).apply()

    fun clearWallpaper(context: Context, target: WallpaperTarget) {
        when (target) {
            WallpaperTarget.HOME -> setHomeWallpaperUri(context, null)
            WallpaperTarget.NOTIFICATIONS -> setNotificationsWallpaperUri(context, null)
            WallpaperTarget.SETTINGS -> setSettingsWallpaperUri(context, null)
        }
    }
}

enum class WallpaperTarget {
    HOME,
    NOTIFICATIONS,
    SETTINGS
}
