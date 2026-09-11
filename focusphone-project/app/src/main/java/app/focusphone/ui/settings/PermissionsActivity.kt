package app.focusphone.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import app.focusphone.R
import app.focusphone.databinding.ActivityPermissionsBinding
import app.focusphone.service.FocusAccessibilityService
import app.focusphone.ui.base.BaseActivity
import app.focusphone.util.LauncherUtils
import androidx.core.view.updatePadding
import kotlinx.coroutines.launch

class PermissionsActivity : BaseActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        binding.btnBack.setOnClickListener { finish() }
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    private fun refreshStatuses() {
        // Query All Packages: this is an install-time permission, so checkSelfPermission()
        // is always PERMISSION_GRANTED as long as it's declared in the manifest — it can
        // never reflect a real "denied" state. What actually matters to the user is whether
        // the launcher can see other apps at all (FP-059).
        lifecycleScope.launch {
            val visibleAppCount = focusPhoneApp.container.appRepository.getInstalledApps().size
            binding.tvQueryAllStatus.text = getString(
                if (visibleAppCount > MIN_EXPECTED_VISIBLE_APPS) {
                    R.string.permissions_status_granted
                } else {
                    R.string.permissions_status_denied
                }
            )
        }

        // Accessibility Service
        val accessibilityActive = FocusAccessibilityService.isEnabled(this)
        binding.tvAccessibilityStatus.text = getString(
            if (accessibilityActive) R.string.permissions_status_active else R.string.permissions_status_inactive
        )

        // Default Launcher
        val isDefault = LauncherUtils.isDefaultLauncher(this)
        binding.tvLauncherStatus.text = getString(
            if (isDefault) R.string.permissions_status_active else R.string.permissions_status_inactive
        )
    }

    private fun setupClickListeners() {
        binding.btnQueryAllSettings.setOnClickListener {
            openAppDetailsSettings()
        }

        binding.btnAccessibilitySettings.setOnClickListener {
            FocusAccessibilityService.openAccessibilitySettings(this)
        }

        binding.btnLauncherSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun openAppDetailsSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.scrollContent.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private companion object {
        // Any real device has far more than this installed, even freshly set up. A count
        // at or below it means the launcher cannot actually see other apps on the device.
        const val MIN_EXPECTED_VISIBLE_APPS = 5
    }
}
