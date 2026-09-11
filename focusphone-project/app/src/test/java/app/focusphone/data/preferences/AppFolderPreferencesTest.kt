package app.focusphone.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFolderPreferencesTest {

    @Test
    fun `folder membership survives preference manager recreation`() {
        val stored = mutableMapOf<String, String>()
        val first = preferencesBackedBy(stored)
        val folder = requireNotNull(first.createFolder("work"))

        first.moveAppToFolder("com.example.mail", folder.id)
        first.moveAppToFolder("com.example.calendar", folder.id)

        val afterProcessRestart = preferencesBackedBy(stored)
        assertEquals(
            listOf("com.example.mail", "com.example.calendar"),
            afterProcessRestart.getFolder(folder.id)?.packageNames
        )
    }

    @Test
    fun `failed disk write never replaces the in-memory membership`() {
        val stored = mutableMapOf<String, String>()
        val writable = preferencesBackedBy(stored)
        val folder = requireNotNull(writable.createFolder("work"))
        writable.moveAppToFolder("com.example.mail", folder.id)

        val rejecting = AppFolderPreferences(
            readString = stored::get,
            writeString = { _, _ -> false }
        )
        rejecting.moveAppToFolder("com.example.calendar", folder.id)

        assertEquals(
            listOf("com.example.mail"),
            rejecting.getFolder(folder.id)?.packageNames
        )
    }

    private fun preferencesBackedBy(stored: MutableMap<String, String>): AppFolderPreferences =
        AppFolderPreferences(
            readString = stored::get,
            writeString = { key, value ->
                if (value == null) stored.remove(key) else stored[key] = value
                true
            }
        )
}
