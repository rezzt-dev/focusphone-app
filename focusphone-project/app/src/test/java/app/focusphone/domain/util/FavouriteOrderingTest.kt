package app.focusphone.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteOrderingTest {

    @Test
    fun `requested order is applied`() {
        assertEquals(
            listOf("c", "a", "b"),
            FavouriteOrdering.merge(current = listOf("a", "b", "c"), requested = listOf("c", "a", "b"))
        )
    }

    @Test
    fun `favourites missing from the request are kept after it`() {
        // A request built from a truncated list must never delete the rest.
        val current = (1..12).map { "app$it" }
        val visibleReordered = current.take(10).reversed()

        val merged = FavouriteOrdering.merge(current, visibleReordered)

        assertEquals(12, merged.size)
        assertEquals(visibleReordered, merged.take(10))
        assertEquals(listOf("app11", "app12"), merged.drop(10))
    }

    @Test
    fun `empty request keeps the stored order`() {
        assertEquals(listOf("a", "b"), FavouriteOrdering.merge(listOf("a", "b"), emptyList()))
    }

    @Test
    fun `unknown and duplicated packages are ignored`() {
        assertEquals(
            listOf("b", "a"),
            FavouriteOrdering.merge(listOf("a", "b"), listOf("b", "ghost", "b", "a"))
        )
    }
}
