package app.focusphone.ui.theme

import android.app.Application
import android.view.ContextThemeWrapper
import app.focusphone.R
import app.focusphone.data.preferences.ThemePreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Every MaterialAlertDialog in the app is inflated with `FocusPhone.MaterialAlertDialog`.
 *
 * Its title style is applied as the *view* style of a `DialogTitle` that declares no size of
 * its own; when it was parented on a `TextAppearance` every such dialog crashed on inflation,
 * which made an already chosen wallpaper impossible to change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MaterialAlertDialogThemeTest {

    private val context: Application = RuntimeEnvironment.getApplication()

    /** Every style a screen can end up with; the custom and auto themes resolve to these. */
    private val themes = listOf(
        ThemePreferences.THEME_OFF_WHITE,
        ThemePreferences.THEME_SLATE,
        ThemePreferences.THEME_WARM_BEIGE,
        ThemePreferences.THEME_NAVY
    )

    @Test
    fun material_dialogs_inflate_under_every_theme() {
        themes.forEach { theme ->
            val themed = ContextThemeWrapper(context, ThemePreferences.resolveStyleRes(theme))
            val dialog = MaterialAlertDialogBuilder(themed)
                .setTitle(R.string.settings_wallpaper_home)
                .setItems(arrayOf("change", "remove")) { _, _ -> }
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            // Runs the layout inflation that used to throw InflateException.
            dialog.create()

            val title = dialog.findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)
            assertNotNull("dialog title missing under $theme", title)
            assertEquals(
                "dialog title under $theme",
                context.getString(R.string.settings_wallpaper_home),
                title!!.text.toString()
            )
        }
    }
}
