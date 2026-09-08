package app.focusphone.data.preferences

import android.content.Context
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
 * Keys (package names) are validated to ensure they conform to a valid package name shape
 * before persistence.
 */
class CustomAppLabelsPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext
    private val _labelsFlow = MutableStateFlow<Map<String, String>>(loadAllLabels())
    val labelsFlow: StateFlow<Map<String, String>> = _labelsFlow.asStateFlow()

    fun getCustomLabel(packageName: String): String? {
        return _labelsFlow.value[packageName]
    }

    fun setCustomLabel(packageName: String, label: String?) {
        if (!isValidPackageName(packageName)) return

        val current = _labelsFlow.value.toMutableMap()
        if (label.isNullOrBlank()) {
            current.remove(packageName)
            prefs.edit().remove(packageName).apply()
        } else {
            val trimmed = label.trim()
            current[packageName] = trimmed
            SecureStorageHelper.encrypt(appContext, trimmed)?.let { encrypted ->
                prefs.edit().putString(packageName, encrypted).apply()
            }
        }
        _labelsFlow.value = current.toMap()
    }

    private fun loadAllLabels(): Map<String, String> {
        val all = prefs.all
        val map = mutableMapOf<String, String>()
        val plainValuesToMigrate = mutableMapOf<String, String>()

        for ((key, value) in all) {
            if (value !is String) continue
            if (!isValidPackageName(key)) {
                // Clean up invalid keys from older versions
                prefs.edit().remove(key).apply()
                continue
            }

            val decrypted = SecureStorageHelper.decrypt(appContext, value)
            if (decrypted != null) {
                map[key] = decrypted
            } else {
                // Assume legacy plain-text value; migrate transparently
                map[key] = value
                plainValuesToMigrate[key] = value
            }
        }

        if (plainValuesToMigrate.isNotEmpty()) {
            val editor = prefs.edit()
            for ((key, plain) in plainValuesToMigrate) {
                SecureStorageHelper.encrypt(appContext, plain)?.let { encrypted ->
                    editor.putString(key, encrypted)
                }
            }
            editor.apply()
        }

        return map
    }

    private fun isValidPackageName(packageName: String): Boolean {
        // Minimal validation: must contain at least one dot and only allowed characters.
        if (packageName.isBlank()) return false
        if (packageName.length > 255) return false
        if (!packageName.contains(".")) return false
        return packageName.matches(Regex("^[a-zA-Z0-9._]+$"))
    }

    companion object {
        private const val PREF_FILE = "focusphone_custom_labels"
    }
}
