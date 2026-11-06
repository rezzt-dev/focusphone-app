package app.focusphone.ui.widget.library

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.IntentCompat
import app.focusphone.R
import app.focusphone.ui.base.BaseActivity

/**
 * Invisible stop between a widget tap and the app it opens.
 *
 * A widget may live in any launcher, but an app opened from it must still obey the active
 * profile, the app's schedule and the conscious pause. [BaseActivity.openApp] applies all
 * three, so every app a widget opens passes through here and this screen finishes before
 * it is ever drawn.
 *
 * Lint mistakes a screen that finishes in onCreate for a custom splash screen; it is not one.
 */
@SuppressLint("CustomSplashScreen")
class WidgetLaunchActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launch(intent)
        finish()
    }

    private fun launch(intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName != null) {
            openApp(packageName, intent.getStringExtra(EXTRA_LABEL) ?: packageName)
            return
        }
        val target = IntentCompat.getParcelableExtra(intent, EXTRA_TARGET, Intent::class.java) ?: return
        val resolved = resolvePackage(target)
        if (resolved == null) {
            Toast.makeText(this, R.string.widget_no_app_for_action, Toast.LENGTH_SHORT).show()
            return
        }
        openApp(resolved.first, resolved.second)
    }

    /** The package and label of the app that handles [target], or null when there is none. */
    private fun resolvePackage(target: Intent): Pair<String, String>? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(target, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(target, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val activity = info?.activityInfo
        // "android" is the system chooser: there are several apps and no default yet.
        val packageName = activity?.packageName?.takeIf { it != ANDROID_PACKAGE }
            ?: firstHandler(target)
        packageName?.let { it to labelOf(it) }
    } catch (e: RuntimeException) {
        Log.w(TAG, "Could not resolve $target", e)
        null
    }

    private fun firstHandler(target: Intent): String? {
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(target, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(target, 0)
        }
        return matches.firstOrNull()?.activityInfo?.packageName
    }

    private fun labelOf(packageName: String): String = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
        packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    companion object {
        private const val TAG = "WidgetLaunchActivity"
        private const val ANDROID_PACKAGE = "android"
        private const val EXTRA_PACKAGE = "app.focusphone.widget.extra.PACKAGE"
        private const val EXTRA_LABEL = "app.focusphone.widget.extra.LABEL"
        private const val EXTRA_TARGET = "app.focusphone.widget.extra.TARGET"

        fun forApp(context: Context, packageName: String, label: String): Intent =
            Intent(context, WidgetLaunchActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_LABEL, label)

        fun forTarget(context: Context, target: Intent): Intent =
            Intent(context, WidgetLaunchActivity::class.java).putExtra(EXTRA_TARGET, target)
    }
}
