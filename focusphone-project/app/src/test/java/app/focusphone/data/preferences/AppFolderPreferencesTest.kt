package app.focusphone.data.preferences

import app.focusphone.domain.util.AppFolderSerializer
import app.focusphone.security.EncryptedPrefs.StoredValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `temporarily unreadable folders are never overwritten`() {
        val stored = mutableMapOf<String, String>()
        requireNotNull(preferencesBackedBy(stored).createFolder("home"))
        val onDisk = stored.toMap()

        var keyAvailable = false
        val preferences = AppFolderPreferences.forTesting(
            read = { key ->
                if (!keyAvailable) StoredValue.Unavailable
                else stored[key]?.let(StoredValue::Present) ?: StoredValue.Absent
            },
            writeString = { key, value ->
                if (value == null) stored.remove(key) else stored[key] = value
                true
            }
        )

        assertNull(preferences.createFolder("work"))
        assertEquals(onDisk, stored)

        // Once the key is back, the real folders are loaded before the change is applied.
        keyAvailable = true
        assertNotNull(preferences.createFolder("work"))
        assertEquals(listOf("home", "work"), preferences.getFolders().map { it.name })
    }

    @Test
    fun `permanently lost folders can be rebuilt`() {
        val stored = mutableMapOf<String, String>()
        val preferences = AppFolderPreferences.forTesting(
            read = { key -> stored[key]?.let(StoredValue::Present) ?: StoredValue.Lost },
            writeString = { key, value ->
                if (value == null) stored.remove(key) else stored[key] = value
                true
            }
        )

        assertNotNull(preferences.createFolder("work"))
    }

    @Test
    fun `a package left in two folders is repaired on disk`() {
        val stored = mutableMapOf<String, String>()
        val preferences = preferencesBackedBy(stored)
        val first = requireNotNull(preferences.createFolder("a"))
        val second = requireNotNull(preferences.createFolder("b"))
        preferences.moveAppToFolder("com.example.mail", first.id)

        // Simulate an interrupted write that left the app in both folders.
        val healthy = AppFolderSerializer.serializeAll(preferences.getFolders())
        val key = stored.entries.first { it.value == healthy }.key
        stored[key] = AppFolderSerializer.serializeAll(
            preferences.getFolders().map {
                if (it.id == second.id) it.copy(packageNames = listOf("com.example.mail")) else it
            }
        )

        val repaired = preferencesBackedBy(stored)

        assertEquals(1, repaired.getFolders().count { "com.example.mail" in it.packageNames })
        assertEquals(AppFolderSerializer.serializeAll(repaired.getFolders()), stored[key])
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
