package app.focusphone.data.preferences

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import app.focusphone.security.SecureStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encrypted preference manager for user-defined app labels.
 *
 * Values are encrypted at rest with [SecureStorageHelper] (AES-256/GCM + Android Keystore).
 * On first load, any legacy plain-text values are transparently migrated to the encrypted format.
 *
 * Legacy values are recognised by their *shape* ([SecureStorageHelper.looksEncrypted]), never
 * by a failed decryption: when the Keystore key is lost, a real payload also fails to decrypt,
 * and treating it as plain text would re-encrypt the ciphertext and corrupt the label forever.
 * Unreadable labels are left on disk untouched and the system label is shown instead.
 *
 * Keys (package names) are validated to ensure they conform to a valid package name shape
 * before persistence.
 */
class CustomAppLabelsPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val _labelsFlow = MutableStateFlow<Map<String, String>>(loadAllLabels())
    val labelsFlow: StateFlow<Map<String, String>> = _labelsFlow.asStateFlow()

    fun getCustomLabel(packageName: String): String? = _labelsFlow.value[packageName]

    /**
     * Stores [label] for [packageName]; a blank label restores the system name.
     *
     * @return false when nothing changed because the package name is invalid or the label
     * could not be encrypted. The in-memory state only ever reflects what reached disk.
     */
    fun setCustomLabel(packageName: String, label: String?): Boolean {
        if (!isValidPackageName(packageName)) return false

        if (label.isNullOrBlank()) {
            prefs.edit { remove(packageName) }
            _labelsFlow.value = _labelsFlow.value - packageName
            return true
        }

        val trimmed = label.trim()
        val encrypted = SecureStorageHelper.encrypt(trimmed) ?: return false
        prefs.edit { putString(packageName, encrypted) }
        _labelsFlow.value = _labelsFlow.value + (packageName to trimmed)
        return true
    }

    private fun loadAllLabels(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val plainValuesToMigrate = mutableMapOf<String, String>()
        val invalidKeys = mutableListOf<String>()

        for ((key, value) in prefs.all) {
            if (value !is String) continue
            if (!isValidPackageName(key)) {
                invalidKeys += key
                continue
            }

            if (!SecureStorageHelper.looksEncrypted(value)) {
                // Legacy plain-text label written before encryption existed.
                map[key] = value
                plainValuesToMigrate[key] = value
                continue
            }

            when (val result = SecureStorageHelper.decrypt(value)) {
                is SecureStorageHelper.DecryptResult.Success -> map[key] = result.plainText

                // Never rewrite an unreadable payload: the key may come back, and even if it
                // does not, the user simply sees the system name until they rename the app.
                else -> Log.w(TAG, "Custom label for $key is unreadable ($result); using system label")
            }
        }

        if (invalidKeys.isNotEmpty() || plainValuesToMigrate.isNotEmpty()) {
            prefs.edit {
                // Clean up invalid keys from older versions.
                invalidKeys.forEach { remove(it) }
                for ((key, plain) in plainValuesToMigrate) {
                    SecureStorageHelper.encrypt(plain)?.let { encrypted -> putString(key, encrypted) }
                }
            }
        }

        return map
    }

    private fun isValidPackageName(packageName: String): Boolean {
        // Minimal validation: must contain at least one dot and only allowed characters.
        if (packageName.isBlank()) return false
        if (packageName.length > 255) return false
        if (!packageName.contains(".")) return false
        return PACKAGE_NAME_SHAPE.matches(packageName)
    }

    companion object {
        private const val TAG = "CustomAppLabels"
        private const val PREF_FILE = "focusphone_custom_labels"
        private val PACKAGE_NAME_SHAPE = Regex("^[a-zA-Z0-9._]+$")
    }
}
