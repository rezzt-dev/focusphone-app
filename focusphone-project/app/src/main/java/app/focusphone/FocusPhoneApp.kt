package app.focusphone

import android.app.Application
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.data.repository.PackageChangeMonitor
import app.focusphone.di.AppContainer
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
        PackageChangeMonitor.start(this)
        // Ensure detox state is current on every cold start, not just when HomeActivity resumes.
        ProfilePreferences.expireDetoxIfNeeded(this)
    }
}
