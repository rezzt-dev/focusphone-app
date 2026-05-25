package app.focusphone.domain.usecase

import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.repository.FakeFavouriteAppRepository
import app.focusphone.domain.model.ToggleResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFavouriteAppUseCaseTest {

    private lateinit var repository: FakeFavouriteAppRepository
    private lateinit var useCase: ToggleFavouriteAppUseCase

    @Before
    fun setUp() {
        repository = FakeFavouriteAppRepository()
        useCase = ToggleFavouriteAppUseCase(repository)
    }

    @Test
    fun `adds app when not favourite and below limit`() = runTest {
        val result = useCase("com.example.app", "example")
        assertTrue(result is ToggleResult.Added)
        assertEquals(1, repository.getFavourites().size)
    }

    @Test
    fun `removes app when already favourite`() = runTest {
        useCase("com.example.app", "example")
        val result = useCase("com.example.app", "example")
        assertTrue(result is ToggleResult.Removed)
        assertEquals(0, repository.getFavourites().size)
    }

    @Test
    fun `returns LimitReached when max favourites exceeded`() = runTest {
        repeat(ToggleFavouriteAppUseCase.MAX_FAVOURITES) {
            useCase("com.example.app$it", "app$it")
        }
        val result = useCase("com.example.overflow", "overflow")
        assertTrue(result is ToggleResult.LimitReached)
        assertEquals(ToggleFavouriteAppUseCase.MAX_FAVOURITES, repository.getFavourites().size)
    }

    @Test
    fun `does not exceed limit with rapid toggles`() = runTest {
        // Simulate race-like rapid toggles by pre-filling to limit
        repeat(ToggleFavouriteAppUseCase.MAX_FAVOURITES) {
            repository.addFavourite(FavouriteApp("com.app$it", "app$it", it))
        }
        val result = useCase("com.new", "new")
        assertTrue(result is ToggleResult.LimitReached)
    }
}
