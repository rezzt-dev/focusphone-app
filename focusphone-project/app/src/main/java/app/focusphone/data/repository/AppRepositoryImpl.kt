package app.focusphone.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.repository.AppRepository
import app.focusphone.domain.util.normalizeAppName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepositoryImpl(
    private val context: Context,
    private val customLabels: app.focusphone.data.preferences.CustomAppLabelsPreferences
) : AppRepository {

    private var cachedApps: List<AppInfo>? = null
    private var cacheTimestamp: Long = 0L
    private val cacheTtlMillis = 30_000L

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cache = cachedApps
        if (cache != null && (now - cacheTimestamp) < cacheTtlMillis) {
            return@withContext cache
        }

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
        val result = list.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val custom = customLabels.getCustomLabel(pkg)
            AppInfo(
                packageName = pkg,
                label       = (custom ?: info.loadLabel(pm)?.toString() ?: pkg).normalizeAppName(),
                icon        = null
            )
        }.sortedBy { it.label }

        cachedApps = result
        cacheTimestamp = now
        result
    }

    override fun invalidateCache() {
        cachedApps = null
        cacheTimestamp = 0L
    }
}
