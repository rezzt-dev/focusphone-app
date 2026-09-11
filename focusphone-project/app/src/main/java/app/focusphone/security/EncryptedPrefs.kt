package app.focusphone.security

import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * Thin wrapper around a SharedPreferences file whose values are encrypted at rest
 * with AES-256/GCM through [SecureStorageHelper].
 *
 * Access is intentionally synchronous: FocusPhone reads blocking state on the main
 * thread right before launching an app, so an async store (DataStore) is unsuitable.
 * Callers that read on hot paths — such as the accessibility service — must cache
 * the decrypted values themselves.
 *
 * Every value in the file goes through this class, so the file never mixes plain and
 * encrypted entries.
 */
class EncryptedPrefs(private val fileName: String) {

    /** What [read] found under a key. */
    sealed interface StoredValue {
        /** Nothing is stored under the key. */
        data object Absent : StoredValue

        data class Present(val value: String) : StoredValue

        /**
         * Something is stored but the Keystore key cannot be loaded right now. This is
         * transient: callers holding user-authored data must not overwrite it on the
         * assumption that it is empty.
         */
        data object Unavailable : StoredValue

        /**
         * Something is stored but can never be decrypted again (a damaged payload or a
         * reset Keystore). The value is gone; callers may treat it as absent so the user
         * can rebuild it instead of being locked out of the feature for good.
         */
        data object Lost : StoredValue
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    /** Reads and decrypts [key], reporting absent and unreadable values distinctly. */
    fun read(context: Context, key: String): StoredValue {
        val encrypted = try {
            prefs(context).getString(key, null)
        } catch (e: ClassCastException) {
            // A primitive written by a build that predates this wrapper. It never held
            // user-authored text, so dropping it lets the next write use the right format.
            Log.w(TAG, "Dropping legacy non-string value for $key in $fileName")
            prefs(context).edit { remove(key) }
            null
        } ?: return StoredValue.Absent

        return when (val result = SecureStorageHelper.decrypt(encrypted)) {
            is SecureStorageHelper.DecryptResult.Success -> StoredValue.Present(result.plainText)
            SecureStorageHelper.DecryptResult.KeyUnavailable -> {
                Log.e(TAG, "Value for $key in $fileName is temporarily unavailable")
                StoredValue.Unavailable
            }
            SecureStorageHelper.DecryptResult.Unreadable -> {
                Log.e(TAG, "Value for $key in $fileName can no longer be decrypted")
                StoredValue.Lost
            }
        }
    }

    /** Reads and decrypts [key], falling back to [default] when absent or unreadable. */
    fun getString(context: Context, key: String, default: String?): String? =
        when (val stored = read(context, key)) {
            is StoredValue.Present -> stored.value
            else -> default
        }

    /**
     * Encrypts and stores [value]; a null value removes the key.
     *
     * @return false when encryption failed and nothing was written.
     */
    fun putString(context: Context, key: String, value: String?): Boolean {
        if (value == null) {
            prefs(context).edit { remove(key) }
            return true
        }
        val encrypted = SecureStorageHelper.encrypt(value)
        if (encrypted == null) {
            Log.e(TAG, "Not writing $key to $fileName: encryption failed")
            return false
        }
        prefs(context).edit { putString(key, encrypted) }
        return true
    }

    /**
     * Encrypts and commits [value] before returning.
     *
     * This is reserved for small, user-authored configuration whose in-memory state
     * must never get ahead of the value on disk (for example app-folder membership).
     * Returns false when encryption or the disk commit fails.
     */
    fun commitString(context: Context, key: String, value: String?): Boolean {
        val preferences = prefs(context)
        if (value == null) return preferences.edit().remove(key).commit()

        val encrypted = SecureStorageHelper.encrypt(value)
        if (encrypted == null) {
            Log.e(TAG, "Not committing $key to $fileName: encryption failed")
            return false
        }
        return preferences.edit().putString(key, encrypted).commit()
    }

    fun getLong(context: Context, key: String, default: Long): Long =
        getString(context, key, null)?.toLongOrNull() ?: default

    fun putLong(context: Context, key: String, value: Long): Boolean =
        putString(context, key, value.toString())

    fun getInt(context: Context, key: String, default: Int): Int =
        getString(context, key, null)?.toIntOrNull() ?: default

    fun putInt(context: Context, key: String, value: Int): Boolean =
        putString(context, key, value.toString())

    fun remove(context: Context, vararg keys: String) {
        prefs(context).edit { keys.forEach { remove(it) } }
    }

    private companion object {
        const val TAG = "EncryptedPrefs"
    }
}
