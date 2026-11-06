package app.focusphone.domain.util

import app.focusphone.domain.model.AppFolder
import java.util.UUID

/**
 * Pure serialization helpers for [AppFolder], kept out of the preference layer so the
 * format can be unit tested without Android dependencies.
 *
 * One folder per line: `id|name|pkg1,pkg2,...`. Separators are stripped from the name
 * on the way in ([sanitizeName]), so a name can never break the format.
 */
object AppFolderSerializer {

    private const val FIELD_SEPARATOR = "|"
    private const val PACKAGE_SEPARATOR = ","
    private const val LINE_SEPARATOR = "\n"
    private const val FIELD_COUNT = 3

    /** Alphabetical, case-insensitive order — the order folders are shown in. */
    val NAME_ORDER: Comparator<AppFolder> =
        Comparator { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name) }

    /** Generates the identifier of a brand new folder. */
    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Trims [name] and removes the characters used by the storage format.
     * Returns an empty string when nothing printable is left.
     */
    fun sanitizeName(name: String): String = name
        .replace(FIELD_SEPARATOR, " ")
        .replace(LINE_SEPARATOR, " ")
        .replace("\r", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(AppFolder.MAX_NAME_LENGTH)

    fun serialize(folder: AppFolder): String = listOf(
        folder.id,
        folder.name,
        folder.packageNames.joinToString(PACKAGE_SEPARATOR)
    ).joinToString(FIELD_SEPARATOR)

    fun serializeAll(folders: List<AppFolder>): String =
        folders.joinToString(LINE_SEPARATOR) { serialize(it) }

    /**
     * Parses a line produced by [serialize]. Returns `null` for malformed input so a
     * single corrupted entry can be skipped without discarding every folder.
     */
    fun parse(line: String): AppFolder? {
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size != FIELD_COUNT) return null

        val id = parts[0].trim()
        if (id.isEmpty()) return null

        val name = sanitizeName(parts[1])
        if (name.isEmpty()) return null

        val packageNames = parts[2]
            .split(PACKAGE_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return AppFolder(id = id, name = name, packageNames = packageNames)
    }

    /** Parses a whole store, dropping malformed lines and duplicated identifiers. */
    fun parseAll(raw: String?): List<AppFolder> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(LINE_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { parse(it) }
            .distinctBy { it.id }
    }
}
