package app.focusphone.security

import android.content.Context

/**
 * Thin wrapper around a SharedPreferences file whose values are encrypted at rest
 * with AES-256/GCM through [SecureStorageHelper].
 *
 * Access is intentionally synchronous: FocusPhone reads blocking state on the main
 * thread right before launching an app, so an async store (DataStore) is unsuitable.
 * Callers that read on hot paths — such as the accessibility service — must cache
 * the decrypted values themselves.
 */
class EncryptedPrefs(private val fileName: String) {

    private fun prefs(context: Context) =
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    /** Reads and decrypts [key], falling back to [default] when absent or unreadable. */
    fun getString(context: Context, key: String, default: String?): String? {
        val encrypted = try {
            prefs(context).getString(key, null)
        } catch (e: ClassCastException) {
            // Legacy value stored as a different primitive type (e.g. Long or Set).
            // Remove the stale key so the next write persists in the correct format.
            prefs(context).edit().remove(key).apply()
            null
        } ?: return default
        return SecureStorageHelper.decrypt(context, encrypted) ?: default
    }

    /** Encrypts and stores [value]; a null value removes the key. */
    fun putString(context: Context, key: String, value: String?) {
        if (value == null) {
            prefs(context).edit().remove(key).apply()
            return
        }
        val encrypted = SecureStorageHelper.encrypt(context, value) ?: return
        prefs(context).edit().putString(key, encrypted).apply()
    }

    fun getLong(context: Context, key: String, default: Long): Long =
        getString(context, key, null)?.toLongOrNull() ?: default

    fun putLong(context: Context, key: String, value: Long) {
        putString(context, key, value.toString())
    }

    fun getInt(context: Context, key: String, default: Int): Int =
        getString(context, key, null)?.toIntOrNull() ?: default

    fun putInt(context: Context, key: String, value: Int) {
        putString(context, key, value.toString())
    }

    fun remove(context: Context, vararg keys: String) {
        val editor = prefs(context).edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }
}
