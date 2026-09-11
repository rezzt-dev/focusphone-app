package app.focusphone.ui.allapps

import app.focusphone.data.preferences.AppFolderPreferences
import app.focusphone.data.repository.FakeFavouriteAppRepository
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.usecase.GetInstalledAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AllAppsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `incomplete package query does not erase apps from folders`() = runTest(dispatcher) {
        val stored = mutableMapOf<String, String>()
        val folders = AppFolderPreferences(
            readString = stored::get,
            writeString = { key, value ->
                if (value == null) stored.remove(key) else stored[key] = value
                true
            }
        )
        val folder = requireNotNull(folders.createFolder("work"))
        folders.moveAppToFolder("com.example.visible", folder.id)
        folders.moveAppToFolder("com.example.temporarily_missing", folder.id)

        AllAppsViewModel(
            getInstalledApps = GetInstalledAppsUseCase(
                appRepository = FixedAppRepository(
                    listOf(AppInfo("com.example.visible", "visible"))
                ),
                favouriteRepository = FakeFavouriteAppRepository()
            ),
            folderPreferences = folders
        )
        advanceUntilIdle()

        // Simulate a new process reading the value committed by the previous one.
        val afterRestart = AppFolderPreferences(
            readString = stored::get,
            writeString = { key, value ->
                if (value == null) stored.remove(key) else stored[key] = value
                true
            }
        )
        assertEquals(
            listOf("com.example.visible", "com.example.temporarily_missing"),
            afterRestart.getFolder(folder.id)?.packageNames
        )
    }

    private class FixedAppRepository(
        private val apps: List<AppInfo>
    ) : AppRepository {
        override suspend fun getInstalledApps(): List<AppInfo> = apps
        override fun invalidateCache() = Unit
    }
}
