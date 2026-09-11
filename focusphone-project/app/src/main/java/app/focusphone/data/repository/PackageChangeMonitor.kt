package app.focusphone.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide signal that the set of installed apps changed.
 *
 * [version] increases on every install, uninstall, update or enable/disable, so caches of
 * the app list can tell they are stale and open screens can reload — installing an app
 * from the store and coming back to the drawer now shows it straight away.
 */
object PackageChangeMonitor {

    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    private var started = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _version.update { it + 1 }
        }
    }

    /** Starts listening for package broadcasts. Idempotent. */
    fun start(context: Context) {
        synchronized(this) {
            if (started) return
            started = true
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        // Package broadcasts come from the system, which reaches non-exported receivers.
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
