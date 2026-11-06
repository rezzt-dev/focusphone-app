package app.focusphone.ui.allapps

import app.focusphone.domain.model.AppFolder
import app.focusphone.domain.model.AppInfo

/**
 * A single line of the "all apps" drawer.
 *
 * The list mixes folder headers and apps: folders come first (alphabetically), each
 * optionally followed by the apps it holds while it is expanded, and the apps that
 * belong to no folder close the list.
 */
sealed class AllAppsRow {

    /** Stable identity used by DiffUtil; a package can only appear once. */
    abstract val id: String

    data class FolderRow(
        val folder: AppFolder,
        val appCount: Int,
        val expanded: Boolean
    ) : AllAppsRow() {
        override val id: String get() = "folder:${folder.id}"
    }

    /** [folderId] is null when the app sits at the root of the list. */
    data class AppRow(
        val app: AppInfo,
        val folderId: String?
    ) : AllAppsRow() {
        override val id: String get() = "app:${app.packageName}"
    }
}
