package app.focusphone.data.preferences

import android.content.Context

/**
 * Synchronous preferences for the home-screen component layout.
 * Stores the order, visibility and relative sizes of the three main sections.
 */
object HomeLayoutPreferences {

    private const val PREF_FILE = "focusphone_home_layout"

    private const val KEY_ORDER = "component_order"
    private const val KEY_CLOCK_ENABLED = "clock_enabled"
    private const val KEY_WIDGET_ENABLED = "widget_enabled"
    private const val KEY_CLOCK_WEIGHT_PERCENT = "clock_weight_percent"
    private const val KEY_WIDGET_WEIGHT_PERCENT = "widget_weight_percent"
    private const val KEY_CLOCK_TYPE_BEFORE_HIDDEN = "clock_type_before_hidden"
    private const val KEY_APP_SPACING_DP = "app_spacing_dp"
    private const val KEY_CLOCK_FIXED_TOP = "clock_fixed_top"

    private const val DEFAULT_ORDER = "clock,widget,apps"
    private const val MIN_WEIGHT_PERCENT = 10
    private const val MAX_WEIGHT_PERCENT = 80

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
        prefs(context).edit().putString(KEY_ORDER, raw).apply()
    }

    fun isClockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CLOCK_ENABLED, true)

    fun setClockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CLOCK_ENABLED, enabled).apply()
    }

    fun isWidgetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WIDGET_ENABLED, true)

    fun setWidgetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WIDGET_ENABLED, enabled).apply()
    }

    /** Relative size for the clock container, in percent (10-80). */
    fun getClockWeightPercent(context: Context): Int {
        val value = prefs(context).getInt(KEY_CLOCK_WEIGHT_PERCENT, 38)
        return value.coerceIn(MIN_WEIGHT_PERCENT, MAX_WEIGHT_PERCENT)
    }

    fun setClockWeightPercent(context: Context, percent: Int) {
        val coerced = percent.coerceIn(MIN_WEIGHT_PERCENT, MAX_WEIGHT_PERCENT)
        prefs(context).edit().putInt(KEY_CLOCK_WEIGHT_PERCENT, coerced).apply()
    }

    /** Relative size for the external widget container, in percent (10-80). */
    fun getWidgetWeightPercent(context: Context): Int {
        val value = prefs(context).getInt(KEY_WIDGET_WEIGHT_PERCENT, 25)
        return value.coerceIn(MIN_WEIGHT_PERCENT, MAX_WEIGHT_PERCENT)
    }

    fun setWidgetWeightPercent(context: Context, percent: Int) {
        val coerced = percent.coerceIn(MIN_WEIGHT_PERCENT, MAX_WEIGHT_PERCENT)
        prefs(context).edit().putInt(KEY_WIDGET_WEIGHT_PERCENT, coerced).apply()
    }

    /** Stores the clock type before the user hid it so we can restore it later. */
    fun getClockTypeBeforeHidden(context: Context): app.focusphone.domain.model.ClockType {
        val key = prefs(context).getString(KEY_CLOCK_TYPE_BEFORE_HIDDEN, app.focusphone.domain.model.ClockType.DIGITAL.key)
            ?: app.focusphone.domain.model.ClockType.DIGITAL.key
        return app.focusphone.domain.model.ClockType.fromKey(key)
    }

    fun setClockTypeBeforeHidden(context: Context, type: app.focusphone.domain.model.ClockType) {
        prefs(context).edit().putString(KEY_CLOCK_TYPE_BEFORE_HIDDEN, type.key).apply()
    }

    /** Vertical spacing between favourite-app items, in dp (0-40). */
    fun getAppSpacingDp(context: Context): Int {
        return prefs(context).getInt(KEY_APP_SPACING_DP, 0)
    }

    fun setAppSpacingDp(context: Context, spacing: Int) {
        val coerced = spacing.coerceIn(0, 40)
        prefs(context).edit().putInt(KEY_APP_SPACING_DP, coerced).apply()
    }

    fun isClockFixedTop(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CLOCK_FIXED_TOP, false)

    fun setClockFixedTop(context: Context, fixed: Boolean) {
        prefs(context).edit().putBoolean(KEY_CLOCK_FIXED_TOP, fixed).apply()
    }

    /** Reset everything to factory defaults. */
    fun resetToDefaults(context: Context) {
        prefs(context).edit()
            .putString(KEY_ORDER, DEFAULT_ORDER)
            .putBoolean(KEY_CLOCK_ENABLED, true)
            .putBoolean(KEY_WIDGET_ENABLED, true)
            .putInt(KEY_CLOCK_WEIGHT_PERCENT, 38)
            .putInt(KEY_WIDGET_WEIGHT_PERCENT, 25)
            .putInt(KEY_APP_SPACING_DP, 0)
            .putBoolean(KEY_CLOCK_FIXED_TOP, false)
            .apply()
    }

    private fun defaultOrder(): List<Component> =
        listOf(Component.CLOCK, Component.WIDGET, Component.APPS)
}
