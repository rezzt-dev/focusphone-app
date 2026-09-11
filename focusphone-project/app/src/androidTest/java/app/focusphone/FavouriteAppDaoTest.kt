package app.focusphone

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.data.local.AppDatabase
import app.focusphone.data.local.FavouriteAppDao
import app.focusphone.data.model.FavouriteApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reordering against a real Room database: a reorder built from a partial list — the
 * old home screen only ever showed ten favourites — must never delete the rest.
 */
@RunWith(AndroidJUnit4::class)
class FavouriteAppDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavouriteAppDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.favouriteAppDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun reordering_a_partial_list_keeps_every_favourite() = runBlocking {
        dao.insertAll((0 until 12).map { FavouriteApp("com.app$it", "app$it", it) })
        val visibleReversed = dao.getAll().take(10).map { it.packageName }.reversed()

        dao.applyOrder(visibleReversed)

        val after = dao.getAll()
        assertEquals(12, after.size)
        assertEquals(visibleReversed, after.take(10).map { it.packageName })
        assertEquals(listOf("com.app10", "com.app11"), after.drop(10).map { it.packageName })
        assertEquals((0 until 12).toList(), after.map { it.sortOrder })
    }

    @Test
    fun removing_a_favourite_compacts_the_order() = runBlocking {
        dao.insertAll((0 until 3).map { FavouriteApp("com.app$it", "app$it", it) })

        dao.deleteAndReorder("com.app1")

        val after = dao.getAll()
        assertEquals(listOf("com.app0", "com.app2"), after.map { it.packageName })
        assertEquals(listOf(0, 1), after.map { it.sortOrder })
    }
}
