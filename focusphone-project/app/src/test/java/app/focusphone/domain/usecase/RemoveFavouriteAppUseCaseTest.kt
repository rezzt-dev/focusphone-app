package app.focusphone.domain.usecase

import app.focusphone.data.model.FavouriteApp
import app.focusphone.data.repository.FakeFavouriteAppRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoveFavouriteAppUseCaseTest {

    private val repository = FakeFavouriteAppRepository()
    private val removeFavourite = RemoveFavouriteAppUseCase(repository)

    @Test
    fun `removes a favourite`() = runTest {
        repository.addFavourite(FavouriteApp("com.a", "a"))
        removeFavourite("com.a")
        assertFalse(repository.isFavourite("com.a"))
    }

    @Test
    fun `removing twice never adds the app back`() = runTest {
        repository.addFavourite(FavouriteApp("com.a", "a"))
        // Two overlapping clean-ups of the same uninstalled app.
        removeFavourite("com.a")
        removeFavourite("com.a")
        assertFalse(repository.isFavourite("com.a"))
        assertEquals(0, repository.getFavourites().size)
    }
}
