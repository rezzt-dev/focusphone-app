package app.focusphone.domain.util

import app.focusphone.domain.model.AppFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFolderSerializerTest {

    // ── Name sanitization ──────────────────────────────────────────

    @Test
    fun `sanitize trims and collapses whitespace`() {
        assertEquals("social apps", AppFolderSerializer.sanitizeName("  social   apps  "))
    }

    @Test
    fun `sanitize strips the characters used by the storage format`() {
        assertEquals("work stuff", AppFolderSerializer.sanitizeName("work|stuff"))
        assertEquals("work stuff", AppFolderSerializer.sanitizeName("work\nstuff"))
    }

    @Test
    fun `sanitize caps the name length`() {
        val long = "a".repeat(AppFolder.MAX_NAME_LENGTH + 20)
        assertEquals(AppFolder.MAX_NAME_LENGTH, AppFolderSerializer.sanitizeName(long).length)
    }

    @Test
    fun `sanitize returns empty for blank input`() {
        assertEquals("", AppFolderSerializer.sanitizeName("   \n  "))
    }

    // ── Round trip ─────────────────────────────────────────────────

    @Test
    fun `serialize and parse restore the folder`() {
        val folder = AppFolder("id-1", "work", listOf("com.a", "com.b"))
        assertEquals(folder, AppFolderSerializer.parse(AppFolderSerializer.serialize(folder)))
    }

    @Test
    fun `empty folder round trips`() {
        val folder = AppFolder("id-1", "work", emptyList())
        assertEquals(folder, AppFolderSerializer.parse(AppFolderSerializer.serialize(folder)))
    }

    @Test
    fun `a name carrying a separator cannot break the format`() {
        val folder = AppFolder("id-1", AppFolderSerializer.sanitizeName("work|life"), listOf("com.a"))
        val parsed = AppFolderSerializer.parse(AppFolderSerializer.serialize(folder))
        assertEquals("work life", parsed?.name)
        assertEquals(listOf("com.a"), parsed?.packageNames)
    }

    @Test
    fun `parse drops duplicated packages inside a folder`() {
        val parsed = AppFolderSerializer.parse("id-1|work|com.a,com.b,com.a")
        assertEquals(listOf("com.a", "com.b"), parsed?.packageNames)
    }

    // ── Malformed input ────────────────────────────────────────────

    @Test
    fun `parse rejects malformed lines`() {
        assertNull(AppFolderSerializer.parse(""))
        assertNull(AppFolderSerializer.parse("id-1|work"))
        assertNull(AppFolderSerializer.parse("id-1|work|com.a|extra"))
        assertNull(AppFolderSerializer.parse("|work|com.a"))
        assertNull(AppFolderSerializer.parse("id-1|   |com.a"))
    }

    @Test
    fun `parseAll skips corrupted lines instead of discarding everything`() {
        val raw = "id-1|work|com.a\nbroken line\nid-2|games|com.b"
        val folders = AppFolderSerializer.parseAll(raw)
        assertEquals(listOf("id-1", "id-2"), folders.map { it.id })
    }

    @Test
    fun `parseAll drops folders sharing an identifier`() {
        val folders = AppFolderSerializer.parseAll("id-1|work|com.a\nid-1|games|com.b")
        assertEquals(1, folders.size)
        assertEquals("work", folders.first().name)
    }

    @Test
    fun `parseAll handles an absent store`() {
        assertEquals(emptyList<AppFolder>(), AppFolderSerializer.parseAll(null))
        assertEquals(emptyList<AppFolder>(), AppFolderSerializer.parseAll("   "))
    }

    // ── Ordering ───────────────────────────────────────────────────

    @Test
    fun `folders are ordered alphabetically ignoring case`() {
        val folders = listOf(
            AppFolder("1", "Zen"),
            AppFolder("2", "art"),
            AppFolder("3", "Books")
        ).sortedWith(AppFolderSerializer.NAME_ORDER)

        assertEquals(listOf("art", "Books", "Zen"), folders.map { it.name })
    }

    @Test
    fun `new identifiers are unique`() {
        assertNotEquals(AppFolderSerializer.newId(), AppFolderSerializer.newId())
    }
}
