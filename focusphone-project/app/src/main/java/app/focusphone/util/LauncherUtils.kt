package app.focusphone.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Platform-safe queries about FocusPhone's launcher role. */
object LauncherUtils {

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
}
