package app.focusphone.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import app.focusphone.R
import app.focusphone.data.preferences.ThemePreferences

/**
 * Base class for all FocusPhone activities.
 *
 * Responsibilities:
 *  - Apply the user-selected theme before [setContentView] (must be first).
 *  - Enable edge-to-edge rendering (transparent status/nav bars).
 *  - Apply consistent fade transitions on every Activity launch.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fp_fade_in, R.anim.fp_fade_out)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fp_fade_in, R.anim.fp_fade_out)
    }

    // ── Theme ──────────────────────────────────────────────────────

    private fun applyTheme() {
        val theme = ThemePreferences.getTheme(this)
        setTheme(ThemePreferences.resolveStyleRes(theme))
    }

    // ── Edge-to-edge ───────────────────────────────────────────────

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarVisibility()
    }

    protected fun applyStatusBarVisibility() {
        val hideStatusBar = ThemePreferences.isStatusBarHidden(this)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (hideStatusBar) {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }
}
