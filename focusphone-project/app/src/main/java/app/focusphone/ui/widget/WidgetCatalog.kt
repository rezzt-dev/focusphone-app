package app.focusphone.ui.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.drawable.Drawable
import app.focusphone.domain.util.FuzzyMatcher

/** One widget provider offered by the picker. */
data class WidgetEntry(
    val info: AppWidgetProviderInfo,
    val provider: ComponentName,
    val label: String,
    val description: String?,
    val appLabel: String,
    val spanX: Int,
    val spanY: Int,
    val inUse: Boolean
) {
    val packageName: String get() = provider.packageName
}

/** A row of the picker list: an app header or one of its widgets. */
sealed interface WidgetPickerRow {
    data class AppHeader(
        val packageName: String,
        val appLabel: String,
        val icon: Drawable?,
        val widgetCount: Int
    ) : WidgetPickerRow

    data class Widget(val entry: WidgetEntry) : WidgetPickerRow
}

/**
 * Turns the installed providers into the picker's rows: grouped by app, apps and widgets
 * in alphabetical order (an optional pinned app first), filtered by a search that matches
 * the widget or the app name.
 */
object WidgetCatalog {

    /**
     * @param pinnedPackage an app listed before all others whatever its name: FocusPhone
     *   itself, whose widgets are made to match the launcher.
     */
    fun rows(
        entries: List<WidgetEntry>,
        query: String,
        appIcons: Map<String, Drawable?> = emptyMap(),
        pinnedPackage: String? = null
    ): List<WidgetPickerRow> {
        val needle = query.trim()
        val visible = if (needle.isEmpty()) {
            entries
        } else {
            entries.filter { FuzzyMatcher.matches(needle, it.label) || FuzzyMatcher.matches(needle, it.appLabel) }
        }

        return visible
            .groupBy { it.packageName }
            .entries
            .sortedWith(compareBy({ it.key != pinnedPackage }, { it.value.first().appLabel.lowercase() }, { it.key }))
            .flatMap { (packageName, widgets) ->
                val sorted = widgets.sortedWith(compareBy({ it.label.lowercase() }, { it.provider.className }))
                buildList {
                    add(
                        WidgetPickerRow.AppHeader(
                            packageName = packageName,
                            appLabel = sorted.first().appLabel,
                            icon = appIcons[packageName],
                            widgetCount = sorted.size
                        )
                    )
                    sorted.forEach { add(WidgetPickerRow.Widget(it)) }
                }
            }
    }
}
