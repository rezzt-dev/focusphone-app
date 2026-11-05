package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit
import app.focusphone.domain.model.ClockType

/**
 * Synchronous preferences for the look of the home-screen blocks. Where each block sits is
 * stored by [HomeGridPreferences].
 */
object HomeLayoutPreferences {

    private const val PREF_FILE = "focusphone_home_layout"

    private const val KEY_CLOCK_TYPE_BEFORE_HIDDEN = "clock_type_before_hidden"
    private const val KEY_APP_SPACING_DP = "app_spacing_dp"

    /** Retired layout settings deleted by [removeObsoleteKeys]. */
    private val OBSOLETE_KEYS = arrayOf(
        "clock_weight_percent", "widget_weight_percent", "clock_fixed_top"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** The clock style to bring back when a hidden clock is added to the home screen again. */
    fun getClockTypeBeforeHidden(context: Context): ClockType {
        val key = prefs(context).getString(KEY_CLOCK_TYPE_BEFORE_HIDDEN, ClockType.DIGITAL.key)
            ?: ClockType.DIGITAL.key
        return ClockType.fromKey(key)
    }

    fun setClockTypeBeforeHidden(context: Context, type: ClockType) {
        prefs(context).edit { putString(KEY_CLOCK_TYPE_BEFORE_HIDDEN, type.key) }
    }

    /** Vertical spacing between favourite-app items, in dp (0-40). */
    fun getAppSpacingDp(context: Context): Int = prefs(context).getInt(KEY_APP_SPACING_DP, 0)

    fun setAppSpacingDp(context: Context, spacing: Int) {
        val coerced = spacing.coerceIn(0, 40)
        prefs(context).edit { putInt(KEY_APP_SPACING_DP, coerced) }
    }

    /**
     * Deletes the keys of settings that no longer exist. Writes only when one is present.
     * The pre-grid order and visibility keys are not listed: [HomeGridPreferences] reads
     * them once to carry the layout over, then deletes them itself.
     */
    fun removeObsoleteKeys(context: Context) {
        val prefs = prefs(context)
        if (OBSOLETE_KEYS.none(prefs::contains)) return
        prefs.edit { OBSOLETE_KEYS.forEach(::remove) }
    }
}
