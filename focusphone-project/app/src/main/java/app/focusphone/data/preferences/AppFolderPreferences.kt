package app.focusphone.data.preferences

import android.content.Context
import app.focusphone.domain.model.AppFolder
import app.focusphone.domain.util.AppFolderSerializer
import app.focusphone.security.EncryptedPrefs
import app.focusphone.security.EncryptedPrefs.StoredValue
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
 * While the Keystore key is temporarily unavailable, the drawer shows no folders and every
 * change is refused until they become readable again: writing on top of that state would
 * replace the user's real folders with whatever was built from the empty fallback. Folders
 * that can never be decrypted again are treated as gone, so new ones can be created.
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
    ) : this(LambdaAppFolderStore({ key -> readString(key)?.let(StoredValue::Present) ?: StoredValue.Absent }, writeString))

    /** true while the stored folders exist but cannot be decrypted yet. */
    private var foldersUnavailable = false

    /** true when the last load had to de-duplicate memberships that are still on disk. */
    private var repairPending = false

    private val _foldersFlow = MutableStateFlow(loadFolders())
    val foldersFlow: StateFlow<List<AppFolder>> = _foldersFlow.asStateFlow()

    private val _expandedFlow = MutableStateFlow(loadExpanded())
    val expandedFlow: StateFlow<Set<String>> = _expandedFlow.asStateFlow()

    init {
        persistRepairIfPending()
    }

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
     * Returns null when the name is blank or already taken, or the folder could not be saved.
     */
    fun createFolder(name: String): AppFolder? {
        if (!ensureReadable()) return null
        val clean = AppFolderSerializer.sanitizeName(name)
        if (clean.isEmpty() || isNameTaken(clean)) return null

        val folder = AppFolder(id = AppFolderSerializer.newId(), name = clean)
        if (!persist(_foldersFlow.value + folder)) return null
        setExpanded(folder.id, true)
        return folder
    }

    /** Renames [folderId]. Returns false when the name is blank or taken, or saving failed. */
    fun renameFolder(folderId: String, name: String): Boolean {
        if (!ensureReadable()) return false
        val clean = AppFolderSerializer.sanitizeName(name)
        if (clean.isEmpty() || isNameTaken(clean, excludeId = folderId)) return false
        if (_foldersFlow.value.none { it.id == folderId }) return false

        return persist(
            _foldersFlow.value.map { if (it.id == folderId) it.copy(name = clean) else it }
        )
    }

    /** Deletes [folderId]; the apps it held simply return to the root list. */
    fun deleteFolder(folderId: String) {
        if (!ensureReadable()) return
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
        if (!ensureReadable()) return
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
        if (!ensureReadable()) return
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

    /**
     * Makes sure the in-memory folders mirror what is on disk before a change is built on
     * top of them. Retries a previously unreadable store, since a Keystore that was only
     * temporarily unavailable may have recovered.
     */
    private fun ensureReadable(): Boolean {
        if (!foldersUnavailable) return true
        val reloaded = loadFolders()
        // Still unavailable (EncryptedPrefs has logged why): refuse rather than overwrite.
        if (foldersUnavailable) return false
        _foldersFlow.value = reloaded
        persistRepairIfPending()
        return true
    }

    /**
     * Writes back a membership list that [loadFolders] had to de-duplicate, so the fix is
     * made once instead of being recomputed — possibly differently — on every start.
     */
    private fun persistRepairIfPending() {
        if (!repairPending) return
        repairPending = false
        persist(_foldersFlow.value)
    }

    private fun persist(folders: List<AppFolder>): Boolean {
        val ordered = folders.sortedWith(AppFolderSerializer.NAME_ORDER)
        if (!store.putString(KEY_FOLDERS, AppFolderSerializer.serializeAll(ordered))) return false
        _foldersFlow.value = ordered
        return true
    }

    private fun loadFolders(): List<AppFolder> {
        val raw = when (val stored = store.read(KEY_FOLDERS)) {
            StoredValue.Absent, StoredValue.Lost -> null
            is StoredValue.Present -> stored.value
            StoredValue.Unavailable -> {
                foldersUnavailable = true
                return emptyList()
            }
        }
        foldersUnavailable = false
        val parsed = AppFolderSerializer.parseAll(raw)

        // Defensive de-duplication: a package must live in a single folder even if an
        // interrupted write left it in two.
        val seen = mutableSetOf<String>()
        val deduplicated = parsed.map { folder ->
            folder.copy(packageNames = folder.packageNames.filter { seen.add(it) })
        }
        repairPending = deduplicated != parsed
        return deduplicated.sortedWith(AppFolderSerializer.NAME_ORDER)
    }

    private fun loadExpanded(): Set<String> =
        (store.read(KEY_EXPANDED) as? StoredValue.Present)?.value
            ?.split(EXPANDED_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    companion object {
        /** Test hook whose store can report any [StoredValue], unavailable included. */
        internal fun forTesting(
            read: (String) -> StoredValue,
            writeString: (String, String?) -> Boolean
        ): AppFolderPreferences = AppFolderPreferences(LambdaAppFolderStore(read, writeString))

        private const val PREF_FILE = "focusphone_app_folders"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_EXPANDED = "expanded_folders"
        private const val EXPANDED_SEPARATOR = ","
    }
}

/** Minimal store boundary that keeps folder-state tests independent of Android Keystore. */
internal interface AppFolderStore {
    fun read(key: String): StoredValue
    fun putString(key: String, value: String?): Boolean
}

private class EncryptedAppFolderStore(
    private val context: Context,
    private val encryptedPrefs: EncryptedPrefs
) : AppFolderStore {
    override fun read(key: String): StoredValue = encryptedPrefs.read(context, key)

    override fun putString(key: String, value: String?): Boolean =
        encryptedPrefs.commitString(context, key, value)
}

private class LambdaAppFolderStore(
    private val read: (String) -> StoredValue,
    private val writeString: (String, String?) -> Boolean
) : AppFolderStore {
    override fun read(key: String): StoredValue = read.invoke(key)

    override fun putString(key: String, value: String?): Boolean = writeString(key, value)
}
