package app.focusphone.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import app.focusphone.data.preferences.CustomAppLabelsPreferences
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.util.normalizeAppName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Installed launcher apps, cached briefly because querying the PackageManager and loading
 * every label is expensive.
 *
 * The cache holds *system* labels only; custom labels are applied on every read, so a
 * rename is visible immediately instead of after the cache expires. It is also dropped as
 * soon as [PackageChangeMonitor] reports an install, uninstall or update.
 */
class AppRepositoryImpl(
    private val context: Context,
    private val customLabels: CustomAppLabelsPreferences
) : AppRepository {

    private class Snapshot(val apps: List<AppInfo>, val takenAt: Long, val packageVersion: Long)

    @Volatile
    private var snapshot: Snapshot? = null

    /** One PackageManager query at a time; concurrent callers share its result. */
    private val loadLock = Mutex()

    private val cacheTtlMillis = 30_000L

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val systemApps = loadLock.withLock {
            snapshot?.takeIf { it.isFresh() }?.apps ?: querySystemApps().also { apps ->
                snapshot = Snapshot(apps, System.currentTimeMillis(), PackageChangeMonitor.version.value)
            }
        }

        val labels = customLabels.labelsFlow.value
        systemApps
            .map { app -> labels[app.packageName]?.let { app.copy(label = it.normalizeAppName()) } ?: app }
            .sortedBy { it.label }
    }

    override fun invalidateCache() {
        snapshot = null
    }

    private fun Snapshot.isFresh(): Boolean =
        packageVersion == PackageChangeMonitor.version.value &&
            System.currentTimeMillis() - takenAt < cacheTtlMillis

    private fun querySystemApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val list: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return list.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            AppInfo(
                packageName = pkg,
                label       = (info.loadLabel(pm)?.toString() ?: pkg).normalizeAppName(),
                icon        = null
            )
        }
    }
}
