package app.focusphone.data.preferences

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomAppLabelsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val _labelsFlow = MutableStateFlow<Map<String, String>>(loadAllLabels())
    val labelsFlow: StateFlow<Map<String, String>> = _labelsFlow.asStateFlow()

    fun getCustomLabel(packageName: String): String? {
        return _labelsFlow.value[packageName]
    }

    fun setCustomLabel(packageName: String, label: String?) {
        val current = _labelsFlow.value.toMutableMap()
        if (label.isNullOrBlank()) {
            current.remove(packageName)
            prefs.edit().remove(packageName).apply()
        } else {
            current[packageName] = label
            prefs.edit().putString(packageName, label).apply()
        }
        _labelsFlow.value = current.toMap()
    }

    private fun loadAllLabels(): Map<String, String> {
        val all = prefs.all
        val map = mutableMapOf<String, String>()
        for ((key, value) in all) {
            if (value is String) {
                map[key] = value
            }
        }
        return map
    }

    companion object {
        private const val PREF_FILE = "focusphone_custom_labels"
    }
}
