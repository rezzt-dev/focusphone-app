package app.focusphone.data.preferences

import android.content.Context
import app.focusphone.data.model.FavouriteApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed persistence for favourite apps.
 * Uses a pipe-delimited flat format to avoid needing a JSON library.
 * Format per line: "packageName|label|sortOrder"
 *
 * This is a Phase-1 implementation. Room + KSP will replace it in Phase 2
 * once AGP 9.x annotation-processing compatibility is resolved.
 */
class FavouriteAppsPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    private val _appsFlow = MutableStateFlow<List<FavouriteApp>>(loadFromPrefs())

    /** Reactive stream of persisted favourite apps. */
    val appsFlow: StateFlow<List<FavouriteApp>> = _appsFlow.asStateFlow()

    // ── Read ──────────────────────────────────────────────────────

    fun getAll(): List<FavouriteApp> = _appsFlow.value

    fun contains(packageName: String): Boolean =
        _appsFlow.value.any { it.packageName == packageName }

    // ── Write ─────────────────────────────────────────────────────

    fun add(app: FavouriteApp) {
        val current = _appsFlow.value.toMutableList()
        if (current.none { it.packageName == app.packageName }) {
            current.add(app.copy(sortOrder = current.size))
            persist(current)
        }
    }

    fun remove(packageName: String) {
        val updated = _appsFlow.value
            .filter { it.packageName != packageName }
            .mapIndexed { index, app -> app.copy(sortOrder = index) }
        persist(updated)
    }

    fun replaceAll(apps: List<FavouriteApp>) {
        val ordered = apps.mapIndexed { index, app -> app.copy(sortOrder = index) }
        persist(ordered)
    }

    // ── Persistence ───────────────────────────────────────────────

    private fun persist(apps: List<FavouriteApp>) {
        val serialised = apps.joinToString(ROW_SEP) {
            "${it.packageName}$COL_SEP${it.label}$COL_SEP${it.sortOrder}"
        }
        prefs.edit().putString(KEY_APPS, serialised).apply()
        _appsFlow.value = apps
    }

    private fun loadFromPrefs(): List<FavouriteApp> {
        val raw = prefs.getString(KEY_APPS, null) ?: return emptyList()
        return runCatching {
            raw.split(ROW_SEP)
                .filter { it.isNotBlank() }
                .mapIndexed { index, row ->
                    val cols = row.split(COL_SEP)
                    FavouriteApp(
                        packageName = cols[0],
                        label       = cols.getOrElse(1) { cols[0] },
                        sortOrder   = cols.getOrElse(2) { "$index" }.toIntOrNull() ?: index
                    )
                }
                .sortedBy { it.sortOrder }
        }.getOrElse { emptyList() }
    }

    companion object {
        private const val PREF_FILE = "focusphone_favourites"
        private const val KEY_APPS  = "apps"
        private const val ROW_SEP   = "\n"
        private const val COL_SEP   = "|"
    }
}
