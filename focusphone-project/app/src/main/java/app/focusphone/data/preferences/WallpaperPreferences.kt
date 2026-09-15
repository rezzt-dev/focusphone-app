package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Synchronous SharedPreferences storage for wallpaper backgrounds and blur settings.
 *
 * Supports a global enable/disable toggle, a separate image for each wallpaper-aware
 * surface (home screen, notification drawer and settings screens), a shared wallpaper mode,
 * and a blur toggle with intensity slider.
 *
 * The stored references are `file://` URIs of copies kept in app-private storage by
 * [app.focusphone.ui.wallpaper.WallpaperStorage]; builds before that stored `content://`
 * URIs, which are migrated the first time they are displayed.
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
    private const val KEY_READABILITY = "wallpaper_readability"

    private val URI_KEYS = arrayOf(
        KEY_HOME_WALLPAPER_URI,
        KEY_NOTIFICATIONS_WALLPAPER_URI,
        KEY_SETTINGS_WALLPAPER_URI,
        KEY_SHARED_WALLPAPER_URI
    )

    private const val DEFAULT_BLUR_INTENSITY = 50

    /**
     * Default legibility level. Tuned so the settings screens stay comfortably readable
     * over a photo while the home screen keeps most of the wallpaper visible.
     */
    const val DEFAULT_READABILITY = 60

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun isUseSharedWallpaper(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_SHARED_WALLPAPER, false)

    /** Raw flag write. UI code switches modes through [setSharedMode] instead. */
    fun setUseSharedWallpaper(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_USE_SHARED_WALLPAPER, enabled) }

    /**
     * Turns the shared mode on or off without making any wallpaper disappear.
     *
     * The shared image lives in its own slot, which is empty until the mode is first used;
     * flipping only the flag made every screen go blank at once. Turning the mode on
     * therefore starts from the image already on screen (home first), and turning it off
     * gives each screen back its own image, or the shared one when it never had its own.
     *
     * Run it inside [app.focusphone.ui.wallpaper.WallpaperStorage.update], which deletes
     * the copy that stops being referenced.
     */
    fun setSharedMode(context: Context, enabled: Boolean) {
        val prefs = prefs(context)
        fun own(key: String) = prefs.getString(key, null)?.takeIf { it.isNotBlank() }
        val surfaceKeys = arrayOf(KEY_HOME_WALLPAPER_URI, KEY_NOTIFICATIONS_WALLPAPER_URI, KEY_SETTINGS_WALLPAPER_URI)

        prefs.edit {
            if (enabled) {
                if (own(KEY_SHARED_WALLPAPER_URI) == null) {
                    surfaceKeys.firstNotNullOfOrNull(::own)?.let { putString(KEY_SHARED_WALLPAPER_URI, it) }
                }
            } else {
                own(KEY_SHARED_WALLPAPER_URI)?.let { shared ->
                    surfaceKeys.filter { own(it) == null }.forEach { putString(it, shared) }
                }
                remove(KEY_SHARED_WALLPAPER_URI)
            }
            putBoolean(KEY_USE_SHARED_WALLPAPER, enabled)
        }
    }

    fun getSharedWallpaperUri(context: Context): String? =
        prefs(context).getString(KEY_SHARED_WALLPAPER_URI, null)?.takeIf { it.isNotBlank() }

    fun setSharedWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit { putString(KEY_SHARED_WALLPAPER_URI, uri) }

    fun getHomeWallpaperUri(context: Context): String? =
        resolveEffectiveUri(context, KEY_HOME_WALLPAPER_URI)

    fun setHomeWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit { putString(KEY_HOME_WALLPAPER_URI, uri) }

    fun getNotificationsWallpaperUri(context: Context): String? =
        resolveEffectiveUri(context, KEY_NOTIFICATIONS_WALLPAPER_URI)

    fun setNotificationsWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit { putString(KEY_NOTIFICATIONS_WALLPAPER_URI, uri) }

    fun getSettingsWallpaperUri(context: Context): String? =
        resolveEffectiveUri(context, KEY_SETTINGS_WALLPAPER_URI)

    fun setSettingsWallpaperUri(context: Context, uri: String?) =
        prefs(context).edit { putString(KEY_SETTINGS_WALLPAPER_URI, uri) }

    /** Every image reference currently stored, whichever surface or mode it belongs to. */
    fun storedUris(context: Context): Set<String> {
        val prefs = prefs(context)
        return URI_KEYS.mapNotNull { prefs.getString(it, null)?.takeIf { uri -> uri.isNotBlank() } }.toSet()
    }

    /** Points every surface that used [oldUri] at [newUri] instead. */
    fun replaceUri(context: Context, oldUri: String, newUri: String) {
        val prefs = prefs(context)
        prefs.edit {
            URI_KEYS.filter { prefs.getString(it, null) == oldUri }.forEach { putString(it, newUri) }
        }
    }

    /** Forgets [uri] on every surface that used it. */
    fun removeUri(context: Context, uri: String) {
        val prefs = prefs(context)
        prefs.edit {
            URI_KEYS.filter { prefs.getString(it, null) == uri }.forEach { remove(it) }
        }
    }

    private fun resolveEffectiveUri(context: Context, ownKey: String): String? {
        if (isUseSharedWallpaper(context)) {
            return getSharedWallpaperUri(context)
        }
        return prefs(context).getString(ownKey, null)?.takeIf { it.isNotBlank() }
    }

    fun isBlurEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLUR_ENABLED, false)

    fun setBlurEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_BLUR_ENABLED, enabled) }

    fun getBlurIntensity(context: Context): Int =
        prefs(context).getInt(KEY_BLUR_INTENSITY, DEFAULT_BLUR_INTENSITY)
            .coerceIn(0, 100)

    fun setBlurIntensity(context: Context, intensity: Int) =
        prefs(context).edit { putInt(KEY_BLUR_INTENSITY, intensity.coerceIn(0, 100)) }

    /**
     * How strongly FocusPhone protects text drawn over a wallpaper, from 0 (no
     * protection, the raw image everywhere) to 100 (nearly opaque panels).
     *
     * The value is a user intent, not a raw alpha: each surface maps it to its own
     * range in `SurfaceScrim` because a dense settings list and a minimalist home
     * screen need very different amounts of help.
     */
    fun getReadability(context: Context): Int =
        prefs(context).getInt(KEY_READABILITY, DEFAULT_READABILITY).coerceIn(0, 100)

    fun setReadability(context: Context, level: Int) =
        prefs(context).edit { putInt(KEY_READABILITY, level.coerceIn(0, 100)) }

    fun isWallpaperEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WALLPAPER_ENABLED, true)

    fun setWallpaperEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_WALLPAPER_ENABLED, enabled) }

    /**
     * Stores [uri] as the image of [target], or removes it when null. While the shared
     * wallpaper mode is on every surface shows one image, so that image is the one changed.
     */
    fun assignWallpaper(context: Context, target: WallpaperTarget, uri: String?) {
        if (isUseSharedWallpaper(context)) {
            setSharedWallpaperUri(context, uri)
            return
        }
        when (target) {
            WallpaperTarget.HOME -> setHomeWallpaperUri(context, uri)
            WallpaperTarget.NOTIFICATIONS -> setNotificationsWallpaperUri(context, uri)
            WallpaperTarget.SETTINGS -> setSettingsWallpaperUri(context, uri)
        }
    }
}

enum class WallpaperTarget {
    HOME,
    NOTIFICATIONS,
    SETTINGS
}
