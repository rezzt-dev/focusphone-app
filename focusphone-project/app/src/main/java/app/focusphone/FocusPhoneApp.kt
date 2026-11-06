package app.focusphone

import android.app.Application
import android.content.res.Configuration
import app.focusphone.data.preferences.HomeLayoutPreferences
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.repository.PackageChangeMonitor
import app.focusphone.di.AppContainer
import app.focusphone.ui.widget.library.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application entry point.
 * Holds the [AppContainer] which acts as the manual dependency graph.
 * Hilt/Dagger will replace this in Phase 2 once AGP compatibility is resolved.
 */
open class FocusPhoneApp : Application() {

    /** Lazily initialized dependency container — single source of truth for all singletons. */
    open val container: AppContainer by lazy { AppContainer(this, applicationScope) }

    /** Application-wide coroutine scope for data migrations and background tasks. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ProfilePreferences.migrateLegacyKeys(this)
        HomeLayoutPreferences.removeObsoleteKeys(this)
        PackageChangeMonitor.start(this)
        // Ensure detox state is current on every cold start, not just when HomeActivity resumes.
        ProfilePreferences.expireDetoxIfNeeded(this)
        // FocusPhone's own widgets: re-arm their midnight tick and battery / flashlight
        // watchers, which do not survive the process.
        WidgetUpdater.onWidgetsChanged(this)
    }

    /** Day/night changes repaint the widgets when the theme follows the system. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        WidgetUpdater.refreshIfStyleChanged(this)
    }
}
