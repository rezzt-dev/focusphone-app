package app.focusphone.data.preferences

import android.content.Context
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ThemePreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Clear preferences before each test
        context.getSharedPreferences("focusphone_theme", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun `getTheme returns default SLATE`() {
        val theme = ThemePreferences.getTheme(context)
        assertEquals(ThemePreferences.THEME_SLATE, theme)
    }

    @Test
    fun `setTheme persists value`() {
        ThemePreferences.setTheme(context, ThemePreferences.THEME_NAVY)
        assertEquals(ThemePreferences.THEME_NAVY, ThemePreferences.getTheme(context))
    }

    @Test
    fun `resolveStyleRes returns correct styles for each theme`() {
        val offWhite = ThemePreferences.resolveStyleRes(ThemePreferences.THEME_OFF_WHITE)
        val slate = ThemePreferences.resolveStyleRes(ThemePreferences.THEME_SLATE)
        val warmBeige = ThemePreferences.resolveStyleRes(ThemePreferences.THEME_WARM_BEIGE)
        val navy = ThemePreferences.resolveStyleRes(ThemePreferences.THEME_NAVY)

        assertEquals(app.focusphone.R.style.Theme_FocusPhone_OffWhite, offWhite)
        assertEquals(app.focusphone.R.style.Theme_FocusPhone_Slate, slate)
        assertEquals(app.focusphone.R.style.Theme_FocusPhone_WarmBeige, warmBeige)
        assertEquals(app.focusphone.R.style.Theme_FocusPhone_Navy, navy)
    }

    @Test
    fun `resolveStyleRes returns SLATE for unknown theme`() {
        val result = ThemePreferences.resolveStyleRes("unknown_theme")
        assertEquals(app.focusphone.R.style.Theme_FocusPhone_Slate, result)
    }

    @Test
    fun `getEffectiveTheme resolves AUTO to light when day mode active`() {
        ThemePreferences.setTheme(context, ThemePreferences.THEME_AUTO)

        val config = Configuration(context.resources.configuration)
        config.uiMode = Configuration.UI_MODE_NIGHT_NO
        val newContext = context.createConfigurationContext(config)

        val effective = ThemePreferences.getEffectiveTheme(newContext)
        assertEquals(ThemePreferences.THEME_OFF_WHITE, effective)
    }

    @Test
    fun `getEffectiveTheme resolves AUTO to dark when night mode active`() {
        ThemePreferences.setTheme(context, ThemePreferences.THEME_AUTO)

        val config = Configuration(context.resources.configuration)
        config.uiMode = Configuration.UI_MODE_NIGHT_YES
        val newContext = context.createConfigurationContext(config)

        val effective = ThemePreferences.getEffectiveTheme(newContext)
        assertEquals(ThemePreferences.THEME_SLATE, effective)
    }

    @Test
    fun `getEffectiveTheme returns concrete theme when not AUTO`() {
        ThemePreferences.setTheme(context, ThemePreferences.THEME_NAVY)
        val effective = ThemePreferences.getEffectiveTheme(context)
        assertEquals(ThemePreferences.THEME_NAVY, effective)
    }

    @Test
    fun `custom color parses valid hex correctly`() {
        ThemePreferences.setCustomBackgroundColorHex(context, "#FF5733")
        val color = ThemePreferences.getCustomBackgroundColor(context)
        assertEquals(0xFFFF5733.toInt(), color)
    }

    @Test
    fun `custom color fallback on invalid hex`() {
        ThemePreferences.setCustomBackgroundColorHex(context, "not-a-hex")
        val color = ThemePreferences.getCustomBackgroundColor(context)
        assertEquals(0xFF181818.toInt(), color)
    }

    @Test
    fun `custom foreground fallback on invalid hex`() {
        ThemePreferences.setCustomForegroundColorHex(context, "#ZZZ")
        val color = ThemePreferences.getCustomForegroundColor(context)
        assertEquals(0xFFFAFAFA.toInt(), color)
    }
}
