package app.focusphone.ui.widget.library

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.annotation.ColorInt
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette

/**
 * The colours every FocusPhone widget is painted with, derived from the launcher's active
 * theme so a widget always looks like a piece of the home screen it sits on.
 *
 * Widgets are monochrome on purpose, like the rest of FocusPhone: a raised [card] in the
 * theme's own tone, text in the theme's text colours and a single [accent] for what is
 * active (today's date, the playing button, a filled progress).
 */
data class WidgetStyle(
    /** Fill of the widget body. */
    @param:ColorInt val card: Int,
    /** Fill of buttons, chips and inputs inside the card. */
    @param:ColorInt val chip: Int,
    /** Empty part of rings and bars. */
    @param:ColorInt val track: Int,
    @param:ColorInt val textPrimary: Int,
    @param:ColorInt val textSecondary: Int,
    /** Quiet text: other months' days, units, separators. */
    @param:ColorInt val textTertiary: Int,
    /** Selected state and filled progress. */
    @param:ColorInt val accent: Int,
    /** Content drawn on top of [accent]. */
    @param:ColorInt val onAccent: Int,
    val isDark: Boolean
) {

    companion object {

        /**
         * Share of the text colour mixed into the theme's surface tone, per background
         * tone. Starting from the surface keeps each theme's own tint — warm beige stays
         * warm, navy stays blue — instead of a neutral grey card.
         */
        private const val CARD_DARK = 0.05f
        private const val CARD_LIGHT = 0.025f
        private const val CHIP_DARK = 0.11f
        private const val CHIP_LIGHT = 0.075f
        private const val TRACK_DARK = 0.16f
        private const val TRACK_LIGHT = 0.13f
        private const val TERTIARY = 0.42f

        /** Resolves the style of the theme FocusPhone is using right now. */
        fun resolve(context: Context): WidgetStyle = from(FocusPalette.of(themedContext(context)))

        /** Pure mapping from a launcher palette, kept separate so it can be unit tested. */
        fun from(palette: FocusPalette): WidgetStyle {
            val dark = palette.isDark
            val base = palette.surface
            val card = FocusColors.blend(palette.textPrimary, base, if (dark) CARD_DARK else CARD_LIGHT)
            return WidgetStyle(
                card = card,
                chip = FocusColors.blend(palette.textPrimary, base, if (dark) CHIP_DARK else CHIP_LIGHT),
                track = FocusColors.blend(palette.textPrimary, base, if (dark) TRACK_DARK else TRACK_LIGHT),
                textPrimary = palette.textPrimary,
                textSecondary = palette.textSecondary,
                textTertiary = FocusColors.blend(palette.textPrimary, card, TERTIARY),
                accent = palette.accent,
                onAccent = palette.onAccent,
                isDark = dark
            )
        }

        /**
         * A fingerprint of everything the style depends on. Widgets are repainted only when
         * it changes, so returning to the home screen does not redraw them for nothing.
         */
        fun signature(context: Context): String {
            val theme = ThemePreferences.getTheme(context)
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val custom = if (theme == ThemePreferences.THEME_CUSTOM) {
                ThemePreferences.getCustomBackgroundColorHex(context) + ThemePreferences.getCustomForegroundColorHex(context)
            } else {
                ""
            }
            return "$theme|$night|$custom|${context.resources.configuration.locales[0]}"
        }

        /**
         * Widgets render outside any Activity, so the launcher theme is applied to the
         * application context here; [FocusPalette.of] reads the built-in themes from it.
         */
        private fun themedContext(context: Context): Context {
            val app = context.applicationContext
            return ContextThemeWrapper(app, ThemePreferences.resolveStyleRes(app))
        }
    }
}
