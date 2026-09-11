package app.focusphone.ui.pause

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.focusphone.R
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.databinding.ActivityConsciousPauseBinding
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Conscious pause screen.
 * Displays a calm countdown before launching the target app,
 * giving the user a moment to reflect.
 *
 * The countdown only runs while the screen is in the foreground. Leaving it pauses the
 * countdown instead of letting it finish in the background, where Android 10+ silently
 * refuses to start the target app.
 */
class ConsciousPauseActivity : BaseActivity() {

    private lateinit var binding: ActivityConsciousPauseBinding
    private var remainingSeconds: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsciousPauseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()

        val appLabel = intent.getStringExtra("app_label") ?: getString(R.string.app_unknown)
        binding.tvMindfulQuestion.text = getString(R.string.pause_mindful_question, appLabel.lowercase())

        binding.btnBack.setOnClickListener {
            navigateHome()
        }

        remainingSeconds = savedInstanceState?.getInt(KEY_REMAINING, -1) ?: -1
        if (remainingSeconds <= 0) {
            remainingSeconds = ConsciousPausePreferences.getDuration(this)
        }
        startCountdown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_REMAINING, remainingSeconds)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun startCountdown() {
        lifecycleScope.launch {
            // Cancelled when the screen leaves the foreground and restarted from the
            // remaining seconds when it comes back.
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (remainingSeconds > 0) {
                    render(remainingSeconds)
                    delay(1000)
                    remainingSeconds--
                }
                launchTargetApp()
            }
        }
    }

    private fun render(remaining: Int) {
        binding.tvCountdown.text = remaining.toString()
        binding.tvBreathingState.text = getString(
            when (remaining % 4) {
                0, 3 -> R.string.pause_inhale
                else -> R.string.pause_exhale
            }
        )
    }

    private fun launchTargetApp() {
        val targetIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("target_intent", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("target_intent")
        }

        if (targetIntent != null) {
            try {
                startActivity(targetIntent)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.error_app_not_found, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun navigateHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val KEY_REMAINING = "remaining_seconds"
    }
}
