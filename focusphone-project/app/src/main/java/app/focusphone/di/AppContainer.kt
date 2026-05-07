package app.focusphone.di

import android.content.Context
import app.focusphone.data.repository.AppRepositoryImpl
import app.focusphone.data.repository.FavouriteAppRepositoryImpl
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.repository.FavouriteAppRepository
import app.focusphone.domain.usecase.GetFavouriteAppsUseCase
import app.focusphone.domain.usecase.GetInstalledAppsUseCase
import app.focusphone.domain.usecase.ToggleFavouriteAppUseCase

/**
 * Manual dependency injection container.
 * Replaces Hilt for Phase 1 — avoids AGP 9.x annotation-processing compatibility issues.
 * All singletons are lazily initialised once.
 *
 * TODO Phase 2: migrate to Hilt once compatible version is available for AGP 9.x.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    // ── Data ─────────────────────────────────────────────────────

    val customAppLabelsPreferences: app.focusphone.data.preferences.CustomAppLabelsPreferences by lazy {
        app.focusphone.data.preferences.CustomAppLabelsPreferences(appContext)
    }

    private val database: app.focusphone.data.local.AppDatabase by lazy {
        app.focusphone.data.local.AppDatabase.getDatabase(appContext)
    }

    // ── Repositories ──────────────────────────────────────────────

    val appRepository: AppRepository by lazy {
        AppRepositoryImpl(appContext, customAppLabelsPreferences)
    }

    val favouriteAppRepository: FavouriteAppRepository by lazy {
        FavouriteAppRepositoryImpl(database.favouriteAppDao(), customAppLabelsPreferences, appContext)
    }

    // ── Use Cases ─────────────────────────────────────────────────

    val getInstalledAppsUseCase: GetInstalledAppsUseCase by lazy {
        GetInstalledAppsUseCase(appRepository, favouriteAppRepository)
    }

    val getFavouriteAppsUseCase: GetFavouriteAppsUseCase by lazy {
        GetFavouriteAppsUseCase(favouriteAppRepository)
    }

    val toggleFavouriteAppUseCase: ToggleFavouriteAppUseCase by lazy {
        ToggleFavouriteAppUseCase(favouriteAppRepository)
    }

    val updateAppsOrderUseCase: app.focusphone.domain.usecase.UpdateAppsOrderUseCase by lazy {
        app.focusphone.domain.usecase.UpdateAppsOrderUseCase(favouriteAppRepository)
    }
}
