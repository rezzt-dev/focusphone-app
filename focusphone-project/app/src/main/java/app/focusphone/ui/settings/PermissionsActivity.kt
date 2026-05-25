package app.focusphone.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focusphone.R
import app.focusphone.databinding.ActivityPermissionsBinding
import app.focusphone.service.FocusAccessibilityService
import app.focusphone.ui.base.BaseActivity

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
        // Query All Packages
        val queryGranted = isQueryAllPackagesGranted()
        binding.tvQueryAllStatus.text = getString(
            if (queryGranted) R.string.permissions_status_granted else R.string.permissions_status_denied
        )

        // Accessibility Service
        val accessibilityActive = FocusAccessibilityService.isEnabled(this)
        binding.tvAccessibilityStatus.text = getString(
            if (accessibilityActive) R.string.permissions_status_active else R.string.permissions_status_inactive
        )

        // Default Launcher
        val isDefault = isDefaultLauncher()
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

    private fun isQueryAllPackagesGranted(): Boolean {
        return checkSelfPermission(android.Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
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
            binding.toolbar.setPadding(0, bars.top, 0, 0)
            binding.scrollContent.setPadding(0, 0, 0, bars.bottom)
            insets
        }
    }
}
