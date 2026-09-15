package app.focusphone.domain.usecase

import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.repository.FakeFavouriteAppRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UpdateAppsOrderUseCaseTest {

    private lateinit var repository: FakeFavouriteAppRepository
    private lateinit var useCase: UpdateAppsOrderUseCase

    @Before
    fun setUp() {
        repository = FakeFavouriteAppRepository()
        useCase = UpdateAppsOrderUseCase(repository)
    }

    private suspend fun seed(vararg packages: String) {
        packages.forEach { repository.addFavourite(FavouriteApp(it, it.uppercase())) }
    }

    @Test
    fun `reorders apps and updates sortOrder indices`() = runTest {
        seed("com.a", "com.b", "com.c")

        useCase(
            listOf(
                FavouriteApp("com.c", "C", 2),
                FavouriteApp("com.a", "A", 0),
                FavouriteApp("com.b", "B", 1)
            )
        )

        val result = repository.getFavourites()
        assertEquals(listOf("com.c", "com.a", "com.b"), result.map { it.packageName })
        assertEquals(listOf(0, 1, 2), result.map { it.sortOrder })
    }

    @Test
    fun `reordering a partial list never deletes the other favourites`() = runTest {
        seed("com.a", "com.b", "com.c", "com.d")

        useCase(listOf(FavouriteApp("com.b", "B"), FavouriteApp("com.a", "A")))

        assertEquals(
            listOf("com.b", "com.a", "com.c", "com.d"),
            repository.getFavourites().map { it.packageName }
        )
    }

    @Test
    fun `empty list keeps every favourite`() = runTest {
        seed("com.a")
        useCase(emptyList())
        assertEquals(1, repository.getFavourites().size)
    }
}
