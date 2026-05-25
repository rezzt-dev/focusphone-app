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

    @Test
    fun `reorders apps and updates sortOrder indices`() = runTest {
        val apps = listOf(
            FavouriteApp("com.a", "A", 0),
            FavouriteApp("com.b", "B", 1),
            FavouriteApp("com.c", "C", 2)
        )
        repository.replaceAll(apps)

        val reordered = listOf(
            FavouriteApp("com.c", "C", 2),
            FavouriteApp("com.a", "A", 0),
            FavouriteApp("com.b", "B", 1)
        )

        useCase(reordered)

        val result = repository.getFavourites()
        assertEquals(3, result.size)
        assertEquals("com.c", result[0].packageName)
        assertEquals(0, result[0].sortOrder)
        assertEquals("com.a", result[1].packageName)
        assertEquals(1, result[1].sortOrder)
        assertEquals("com.b", result[2].packageName)
        assertEquals(2, result[2].sortOrder)
    }

    @Test
    fun `empty list clears all favourites`() = runTest {
        repository.addFavourite(FavouriteApp("com.a", "A", 0))
        useCase(emptyList())
        assertEquals(0, repository.getFavourites().size)
    }
}
