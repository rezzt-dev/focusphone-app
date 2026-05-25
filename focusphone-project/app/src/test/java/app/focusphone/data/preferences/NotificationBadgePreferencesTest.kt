package app.focusphone.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class NotificationBadgePreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        NotificationBadgePreferences.clear(context)
    }

    @Test
    fun `getBadgeCount returns zero by default`() {
        assertEquals(0, NotificationBadgePreferences.getBadgeCount(context, "com.example.app"))
    }

    @Test
    fun `setBadgeCounts persists and retrieves correctly`() {
        val counts = mapOf(
            "com.example.messages" to 3,
            "com.example.email" to 12
        )
        NotificationBadgePreferences.setBadgeCounts(context, counts)

        assertEquals(3, NotificationBadgePreferences.getBadgeCount(context, "com.example.messages"))
        assertEquals(12, NotificationBadgePreferences.getBadgeCount(context, "com.example.email"))
        assertEquals(0, NotificationBadgePreferences.getBadgeCount(context, "com.example.other"))
    }

    @Test
    fun `getAllBadgeCounts returns only positive counts`() {
        val counts = mapOf(
            "com.example.a" to 5,
            "com.example.b" to 0,
            "com.example.c" to 1
        )
        NotificationBadgePreferences.setBadgeCounts(context, counts)

        val all = NotificationBadgePreferences.getAllBadgeCounts(context)
        assertEquals(2, all.size)
        assertEquals(5, all["com.example.a"])
        assertEquals(1, all["com.example.c"])
    }

    @Test
    fun `setBadgeCounts replaces previous data`() {
        NotificationBadgePreferences.setBadgeCounts(context, mapOf("com.example.app" to 5))
        NotificationBadgePreferences.setBadgeCounts(context, mapOf("com.example.app" to 2))

        assertEquals(2, NotificationBadgePreferences.getBadgeCount(context, "com.example.app"))
    }

    @Test
    fun `clear removes all counts`() {
        NotificationBadgePreferences.setBadgeCounts(context, mapOf("com.example.app" to 5))
        NotificationBadgePreferences.clear(context)

        assertEquals(0, NotificationBadgePreferences.getBadgeCount(context, "com.example.app"))
        assertEquals(0, NotificationBadgePreferences.getAllBadgeCounts(context).size)
    }
}
