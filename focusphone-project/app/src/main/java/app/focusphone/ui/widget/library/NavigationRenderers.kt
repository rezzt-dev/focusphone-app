package app.focusphone.ui.widget.library

import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import app.focusphone.BuildConfig
import app.focusphone.FocusPhoneApp
import app.focusphone.R
import app.focusphone.data.model.FavouriteApp

/**
 * Widgets that get the user somewhere: the search pill, spotlight (search plus favourites)
 * and the favourites list. Apps are shown by name only, like the home screen.
 */
object NavigationRenderers {

    private const val TAG = "NavigationRenderers"

    val SearchPill = WidgetRenderer { scene ->
        scene.views(R.layout.widget_search_pill).apply {
            bindSearch(scene, fill = scene.style.card)
        }
    }

    val Spotlight = WidgetRenderer { scene ->
        val style = scene.style
        val favourites = favourites(scene.context).take(SPOTLIGHT_CHIPS.size)
        scene.views(R.layout.widget_spotlight).apply {
            card(R.id.wg_bg, style)
            bindSearch(scene, fill = style.chip)
            setTextColor(R.id.wg_fav_header, style.textTertiary)
            setTextColor(R.id.wg_fav_empty, style.textSecondary)
            show(R.id.wg_fav_row, favourites.isNotEmpty())
            show(R.id.wg_fav_empty, favourites.isEmpty())
            SPOTLIGHT_CHIPS.forEachIndexed { index, chip ->
                val app = favourites.getOrNull(index)
                show(chip.frame, app != null)
                if (app == null) return@forEachIndexed
                tint(chip.background, style.chip)
                setTextViewText(chip.label, app.label.lowercase())
                setTextColor(chip.label, style.textPrimary)
                setContentDescription(chip.frame, app.label)
                setOnClickPendingIntent(chip.frame, WidgetIntents.openApp(scene.context, app.packageName, app.label))
            }
            if (favourites.isEmpty()) setOnClickPendingIntent(R.id.wg_fav_empty, WidgetIntents.openHome(scene.context))
        }
    }

    /** As many favourites as the widget's height holds, in the home screen's order. */
    val Favourites = WidgetRenderer { scene ->
        val style = scene.style
        val rows = ((scene.size.heightDp - FAVOURITES_CHROME_DP) / FAVOURITE_ROW_DP).coerceIn(1, FAVOURITE_ROWS.size)
        val favourites = favourites(scene.context).take(rows)
        scene.views(R.layout.widget_favourites).apply {
            card(R.id.wg_bg, style)
            setTextColor(R.id.wg_fav_header, style.textTertiary)
            setTextColor(R.id.wg_fav_empty, style.textSecondary)
            show(R.id.wg_fav_empty, favourites.isEmpty())
            FAVOURITE_ROWS.forEachIndexed { index, id ->
                val app = favourites.getOrNull(index)
                show(id, app != null)
                if (app == null) return@forEachIndexed
                setTextViewText(id, app.label.lowercase())
                setTextColor(id, style.textPrimary)
                setOnClickPendingIntent(id, WidgetIntents.openApp(scene.context, app.packageName, app.label))
            }
            setOnClickPendingIntent(R.id.wg_root, WidgetIntents.openHome(scene.context))
        }
    }

    // ── Shared parts ───────────────────────────────────────────────

    private fun RemoteViews.bindSearch(scene: WidgetScene, fill: Int) {
        val style = scene.style
        tint(R.id.wg_pill_bg, fill)
        tint(R.id.wg_search_icon, style.textSecondary)
        tint(R.id.wg_apps_icon, style.textPrimary)
        setTextColor(R.id.wg_search_hint, style.textSecondary)
        setOnClickPendingIntent(R.id.wg_pill, WidgetIntents.openSearch(scene.context, focusSearch = true))
        setOnClickPendingIntent(R.id.wg_apps, WidgetIntents.openSearch(scene.context, focusSearch = false))
    }

    /** The user's favourites with their custom names, without FocusPhone itself. */
    private suspend fun favourites(context: Context): List<FavouriteApp> = try {
        val app = context.applicationContext as FocusPhoneApp
        app.container.favouriteAppRepository.getFavourites()
            .filter { it.packageName != BuildConfig.APPLICATION_ID }
            .sortedBy { it.sortOrder }
    } catch (e: RuntimeException) {
        Log.e(TAG, "Could not read the favourites", e)
        emptyList()
    }

    private class Chip(val frame: Int, val background: Int, val label: Int)

    private val SPOTLIGHT_CHIPS = listOf(
        Chip(R.id.wg_fav_0, R.id.wg_fav_0_bg, R.id.wg_fav_0_label),
        Chip(R.id.wg_fav_1, R.id.wg_fav_1_bg, R.id.wg_fav_1_label),
        Chip(R.id.wg_fav_2, R.id.wg_fav_2_bg, R.id.wg_fav_2_label),
        Chip(R.id.wg_fav_3, R.id.wg_fav_3_bg, R.id.wg_fav_3_label)
    )

    private val FAVOURITE_ROWS = intArrayOf(R.id.wg_fav_0, R.id.wg_fav_1, R.id.wg_fav_2, R.id.wg_fav_3, R.id.wg_fav_4, R.id.wg_fav_5)

    /** Padding and header of the favourites widget, and the height of one row, in dp. */
    private const val FAVOURITES_CHROME_DP = 52
    private const val FAVOURITE_ROW_DP = 38
}
