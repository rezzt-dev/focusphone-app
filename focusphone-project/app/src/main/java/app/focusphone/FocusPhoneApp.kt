package app.focusphone

import android.app.Application
import app.focusphone.di.AppContainer

/**
 * Application entry point.
 * Holds the [AppContainer] which acts as the manual dependency graph.
 * Hilt/Dagger will replace this in Phase 2 once AGP compatibility is resolved.
 */
class FocusPhoneApp : Application() {

    /** Lazily initialized dependency container — single source of truth for all singletons. */
    val container: AppContainer by lazy { AppContainer(this) }
}
