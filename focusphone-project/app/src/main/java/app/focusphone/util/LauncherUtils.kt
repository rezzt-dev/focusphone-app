package app.focusphone.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log

/** Platform-safe queries about FocusPhone's launcher role. */
object LauncherUtils {

    private const val TAG = "LauncherUtils"

    /**
     * Returns true only when FocusPhone is the user's selected HOME activity.
     *
     * [PackageManager.MATCH_DEFAULT_ONLY] is essential here: without it Android may
     * return the resolver or an arbitrary launcher when no default has been chosen.
     */
    fun isDefaultLauncher(context: Context): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /**
     * Opens the system confirmation to uninstall [packageName].
     *
     * [Intent.ACTION_DELETE] is the supported form of the deprecated
     * `ACTION_UNINSTALL_PACKAGE`; both need `REQUEST_DELETE_PACKAGES`.
     *
     * @return false when the system could not open the confirmation.
     */
    fun requestUninstall(context: Context, packageName: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null)))
        true
    } catch (e: RuntimeException) {
        // ActivityNotFoundException on stripped-down ROMs, SecurityException on some OEMs.
        Log.e(TAG, "Cannot open the uninstall dialog for $packageName", e)
        false
    }
}
