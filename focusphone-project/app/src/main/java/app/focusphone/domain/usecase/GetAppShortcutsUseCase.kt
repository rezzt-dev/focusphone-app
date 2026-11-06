package app.focusphone.domain.usecase

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.util.Log
import app.focusphone.domain.model.AppShortcut
import app.focusphone.util.LauncherUtils

/**
 * Retrieves app shortcuts for a given package using the LauncherApps API.
 *
 * This is the correct API for third-party launchers (requires default launcher status).
 * Returns an empty list if:
 *  - The device runs API < 25,
 *  - FocusPhone is not the default launcher,
 *  - The target app declares no shortcuts,
 *  - The call throws a SecurityException.
 */
class GetAppShortcutsUseCase(private val context: Context) {

    operator fun invoke(packageName: String): List<AppShortcut> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }

        if (!LauncherUtils.isDefaultLauncher(context)) {
            return emptyList()
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return emptyList()

        return try {
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
                setPackage(packageName)
            }

            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle())
                ?: return emptyList()

            shortcuts.mapNotNull { s ->
                val id = s.id ?: return@mapNotNull null
                val label = s.shortLabel?.toString() ?: return@mapNotNull null
                AppShortcut(
                    id = id,
                    packageName = packageName,
                    shortLabel = label,
                    longLabel = s.longLabel?.toString()
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException retrieving shortcuts for $packageName", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve shortcuts for $packageName", e)
            emptyList()
        }
    }

    /**
     * Launches a shortcut via LauncherApps.
     * Returns true if the launch was initiated successfully.
     */
    fun launchShortcut(shortcut: AppShortcut): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return false

        return try {
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcut.id,
                null,
                null,
                Process.myUserHandle()
            )
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException launching shortcut ${shortcut.id}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch shortcut ${shortcut.id}", e)
            false
        }
    }

    companion object {
        private const val TAG = "GetAppShortcutsUseCase"
    }
}
