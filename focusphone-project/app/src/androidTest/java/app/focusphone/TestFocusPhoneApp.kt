package app.focusphone

import app.focusphone.di.AppContainer
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.repository.FavouriteAppRepository

/**
 * Test application that allows injection of fake repositories for instrumented tests.
 *
 * Activities cast [Application] to [FocusPhoneApp], so this class must extend it.
 */
class TestFocusPhoneApp : FocusPhoneApp() {

    var fakeAppRepository: AppRepository? = null
    var fakeFavouriteAppRepository: FavouriteAppRepository? = null

    override val container: AppContainer
        get() = AppContainer(
            context = this,
            applicationScope = applicationScope,
            appRepositoryOverride = fakeAppRepository,
            favouriteAppRepositoryOverride = fakeFavouriteAppRepository
        )

    override fun onCreate() {
        super.onCreate()
        // Skip detox expiration side effects in tests
    }
}
