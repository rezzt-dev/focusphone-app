package app.focusphone

import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.repository.AppRepository

/**
 * Fake [AppRepository] that returns a fixed list of apps for instrumented tests.
 */
class FakeAppRepository(private val apps: List<AppInfo>) : AppRepository {

    override suspend fun getInstalledApps(): List<AppInfo> = apps.toList()

    override fun invalidateCache() {
        // No-op
    }
}
