package app.focusphone.data.preferences

import android.content.Context
import app.focusphone.domain.model.AppFolder
import app.focusphone.domain.util.AppFolderSerializer
import app.focusphone.security.EncryptedPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Synchronous preference manager for the folders of the "all apps" drawer.
 *
 * Folder names reveal how the user organises their phone, so the whole store is
 * encrypted at rest with AES-256/GCM through [EncryptedPrefs], exactly like custom app
 * labels and schedule rules.
 *
 * Two invariants are enforced here rather than in the UI:
 *  - a package belongs to **at most one** folder, so moving an app always removes it
 *    from its previous folder;
 *  - folder names are unique (case-insensitively), so the chooser dialog is never
 *    ambiguous.
 *
 * Membership is not reconciled against a launcher-app query. Android may return an
 * incomplete result during boot, package replacement, or managed-profile changes; an
 * unavailable package remains stored and is hidden by the drawer until it is available
 * again.
 *
 * State is exposed as [StateFlow]s because the drawer rebuilds its rows from the
 * folder set, the expanded set and the installed apps together.
 */
class AppFolderPreferences private constructor(
    private val store: AppFolderStore
) {

    constructor(context: Context) : this(
        EncryptedAppFolderStore(
            context = context.applicationContext,
            encryptedPrefs = EncryptedPrefs(PREF_FILE)
        )
    )

    internal constructor(
        readString: (String) -> String?,
        writeString: (String, String?) -> Boolean
    ) : this(LambdaAppFolderStore(readString, writeString))

    private val _foldersFlow = MutableStateFlow(loadFolders())
    val foldersFlow: StateFlow<List<AppFolder>> = _foldersFlow.asStateFlow()

    private val _expandedFlow = MutableStateFlow(loadExpanded())
    val expandedFlow: StateFlow<Set<String>> = _expandedFlow.asStateFlow()

    // ── Folders ────────────────────────────────────────────────────

    /** Folders in the order they are displayed: alphabetical, case-insensitive. */
    fun getFolders(): List<AppFolder> = _foldersFlow.value

    fun getFolder(folderId: String): AppFolder? =
        _foldersFlow.value.firstOrNull { it.id == folderId }

    /** Returns the folder holding [packageName], or null when the app sits at root. */
    fun folderOf(packageName: String): AppFolder? =
        _foldersFlow.value.firstOrNull { packageName in it.packageNames }

    /** True when another folder already uses [name] (ignoring case and [excludeId]). */
    fun isNameTaken(name: String, excludeId: String? = null): Boolean {
        val candidate = AppFolderSerializer.sanitizeName(name)
        return _foldersFlow.value.any {
            it.id != excludeId && it.name.equals(candidate, ignoreCase = true)
        }
    }

    /**
     * Creates an empty folder named [name].
     * Returns null when the name is blank or already taken.
     */
    fun createFolder(name: String): AppFolder? {
        val clean = AppFolderSerializer.sanitizeName(name)
        if (clean.isEmpty() || isNameTaken(clean)) return null

        val folder = AppFolder(id = AppFolderSerializer.newId(), name = clean)
        if (!persist(_foldersFlow.value + folder)) return null
        setExpanded(folder.id, true)
        return folder
    }

    /** Renames [folderId]. Returns false when the name is blank or already taken. */
    fun renameFolder(folderId: String, name: String): Boolean {
        val clean = AppFolderSerializer.sanitizeName(name)
        if (clean.isEmpty() || isNameTaken(clean, excludeId = folderId)) return false
        if (_foldersFlow.value.none { it.id == folderId }) return false

        return persist(
            _foldersFlow.value.map { if (it.id == folderId) it.copy(name = clean) else it }
        )
    }

    /** Deletes [folderId]; the apps it held simply return to the root list. */
    fun deleteFolder(folderId: String) {
        if (persist(_foldersFlow.value.filterNot { it.id == folderId })) {
            setExpanded(folderId, false)
        }
    }

    // ── Membership ─────────────────────────────────────────────────

    /**
     * Moves [packageName] into [folderId], removing it from any previous folder.
     * Unknown folders are ignored so a stale drag target cannot lose the app.
     */
    fun moveAppToFolder(packageName: String, folderId: String) {
        if (_foldersFlow.value.none { it.id == folderId }) return

        val moved = persist(
            _foldersFlow.value.map { folder ->
                when {
                    folder.id == folderId ->
                        if (packageName in folder.packageNames) folder
                        else folder.copy(packageNames = folder.packageNames + packageName)

                    packageName in folder.packageNames ->
                        folder.copy(packageNames = folder.packageNames - packageName)

                    else -> folder
                }
            }
        )
        if (moved) setExpanded(folderId, true)
    }

    /** Removes [packageName] from whichever folder holds it. */
    fun removeAppFromFolders(packageName: String) {
        if (folderOf(packageName) == null) return
        persist(
            _foldersFlow.value.map { folder ->
                if (packageName in folder.packageNames) {
                    folder.copy(packageNames = folder.packageNames - packageName)
                } else {
                    folder
                }
            }
        )
    }

    // ── Expanded state ─────────────────────────────────────────────

    fun isExpanded(folderId: String): Boolean = folderId in _expandedFlow.value

    fun setExpanded(folderId: String, expanded: Boolean) {
        val current = _expandedFlow.value
        val updated = if (expanded) current + folderId else current - folderId
        if (updated == current) return

        if (store.putString(KEY_EXPANDED, updated.joinToString(EXPANDED_SEPARATOR))) {
            _expandedFlow.value = updated
        }
    }

    fun toggleExpanded(folderId: String) = setExpanded(folderId, !isExpanded(folderId))

    // ── Persistence ────────────────────────────────────────────────

    private fun persist(folders: List<AppFolder>): Boolean {
        val ordered = folders.sortedWith(AppFolderSerializer.NAME_ORDER)
        if (!store.putString(KEY_FOLDERS, AppFolderSerializer.serializeAll(ordered))) return false
        _foldersFlow.value = ordered
        return true
    }

    private fun loadFolders(): List<AppFolder> {
        val parsed = AppFolderSerializer.parseAll(store.getString(KEY_FOLDERS))

        // Defensive de-duplication: a package must live in a single folder even if an
        // interrupted write left it in two.
        val seen = mutableSetOf<String>()
        return parsed
            .map { folder ->
                folder.copy(packageNames = folder.packageNames.filter { seen.add(it) })
            }
            .sortedWith(AppFolderSerializer.NAME_ORDER)
    }

    private fun loadExpanded(): Set<String> =
        store.getString(KEY_EXPANDED)
            ?.split(EXPANDED_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    companion object {
        private const val PREF_FILE = "focusphone_app_folders"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_EXPANDED = "expanded_folders"
        private const val EXPANDED_SEPARATOR = ","
    }
}

/** Minimal store boundary that keeps folder-state tests independent of Android Keystore. */
internal interface AppFolderStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?): Boolean
}

private class EncryptedAppFolderStore(
    private val context: Context,
    private val encryptedPrefs: EncryptedPrefs
) : AppFolderStore {
    override fun getString(key: String): String? =
        encryptedPrefs.getString(context, key, null)

    override fun putString(key: String, value: String?): Boolean =
        encryptedPrefs.commitString(context, key, value)
}

private class LambdaAppFolderStore(
    private val readString: (String) -> String?,
    private val writeString: (String, String?) -> Boolean
) : AppFolderStore {
    override fun getString(key: String): String? = readString(key)

    override fun putString(key: String, value: String?): Boolean = writeString(key, value)
}
