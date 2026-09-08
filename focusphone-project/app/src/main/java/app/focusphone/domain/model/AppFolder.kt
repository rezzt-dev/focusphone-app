package app.focusphone.domain.model

/**
 * A user-created group of apps shown in the "all apps" drawer.
 *
 * Folders are rendered above the plain app list and sorted alphabetically, so no
 * explicit ordering is stored. [packageNames] keeps insertion order only as a
 * persistence detail — the UI always re-sorts the contents by label.
 */
data class AppFolder(
    val id: String,
    val name: String,
    val packageNames: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = packageNames.isEmpty()

    companion object {
        /** Upper bound for a folder name, enforced by the dialog and by the parser. */
        const val MAX_NAME_LENGTH = 30
    }
}
