package app.focusphone

import app.focusphone.di.AppContainer
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.repository.FavouriteAppRepository

/**
 * Test application that allows injection of fake repositories for instrumented tests.
 *
 * Activities cast [Application] to [FocusPhoneApp], so this class must extend it.
 *
 * Like the production app, it exposes a **single** [AppContainer]: every read of
 * [container] returns the same instance, so the `StateFlow`s of labels and folders are
 * shared by every screen under test. The instance is only rebuilt when a fake repository
 * is swapped, because the Application outlives individual tests and a test's `@Before`
 * may install its fakes after an earlier test already built the graph.
 */
class TestFocusPhoneApp : FocusPhoneApp() {

    var fakeAppRepository: AppRepository? = null
        set(value) {
            field = value
            invalidateContainer()
        }

    var fakeFavouriteAppRepository: FavouriteAppRepository? = null
        set(value) {
            field = value
            invalidateContainer()
        }

    @Volatile
    private var cachedContainer: AppContainer? = null

    override val container: AppContainer
        get() = cachedContainer ?: synchronized(this) {
            cachedContainer ?: AppContainer(
                context = this,
                applicationScope = applicationScope,
                appRepositoryOverride = fakeAppRepository,
                favouriteAppRepositoryOverride = fakeFavouriteAppRepository
            ).also { cachedContainer = it }
        }

    private fun invalidateContainer() {
        synchronized(this) { cachedContainer = null }
    }
}
