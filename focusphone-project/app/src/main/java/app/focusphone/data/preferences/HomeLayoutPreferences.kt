package app.focusphone.data.preferences

import android.content.Context
import androidx.core.content.edit
import app.focusphone.domain.model.ClockType

/**
 * Synchronous preferences for the home-screen component layout.
 * Stores the order and visibility of the three main sections.
 */
object HomeLayoutPreferences {

    private const val PREF_FILE = "focusphone_home_layout"

    private const val KEY_ORDER = "component_order"
    private const val KEY_CLOCK_ENABLED = "clock_enabled"
    private const val KEY_WIDGET_ENABLED = "widget_enabled"
    private const val KEY_CLOCK_TYPE_BEFORE_HIDDEN = "clock_type_before_hidden"
    private const val KEY_APP_SPACING_DP = "app_spacing_dp"

    /** Retired layout settings deleted by [removeObsoleteKeys]. */
    private val OBSOLETE_KEYS = arrayOf(
        "clock_weight_percent", "widget_weight_percent", "clock_fixed_top"
    )

    private const val DEFAULT_ORDER = "clock,widget,apps"

    enum class Component(val key: String) {
        CLOCK("clock"),
        WIDGET("widget"),
        APPS("apps")
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** Returns the ordered list of components. */
    fun getOrder(context: Context): List<Component> {
        val raw = prefs(context).getString(KEY_ORDER, DEFAULT_ORDER) ?: DEFAULT_ORDER
        return raw.split(",").mapNotNull { token ->
            Component.entries.firstOrNull { it.key == token.trim() }
        }.ifEmpty { defaultOrder() }
    }

    fun setOrder(context: Context, order: List<Component>) {
        val raw = order.joinToString(",") { it.key }
        prefs(context).edit { putString(KEY_ORDER, raw) }
    }

    fun isClockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CLOCK_ENABLED, true)

    fun setClockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_CLOCK_ENABLED, enabled) }
    }

    fun isWidgetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WIDGET_ENABLED, true)

    fun setWidgetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_WIDGET_ENABLED, enabled) }
    }

    /** Stores the clock type before the user hid it so we can restore it later. */
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

    /** Deletes the keys of settings that no longer exist. Writes only when one is present. */
    fun removeObsoleteKeys(context: Context) {
        val prefs = prefs(context)
        if (OBSOLETE_KEYS.none(prefs::contains)) return
        prefs.edit { OBSOLETE_KEYS.forEach(::remove) }
    }

    private fun defaultOrder(): List<Component> =
        listOf(Component.CLOCK, Component.WIDGET, Component.APPS)
}
