package app.focusphone.ui.pause

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import app.focusphone.R
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.databinding.ActivityConsciousPauseBinding
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Conscious pause screen.
 * Displays a calm countdown before launching the target app,
 * giving the user a moment to reflect.
 */
class ConsciousPauseActivity : BaseActivity() {

    private lateinit var binding: ActivityConsciousPauseBinding
    private var countdownJob: Job? = null
    private var remainingSeconds: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsciousPauseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()

        val appLabel = intent.getStringExtra("app_label") ?: "app"
        binding.tvMindfulQuestion.text = getString(R.string.pause_mindful_question, appLabel.lowercase())

        binding.btnBack.setOnClickListener {
            navigateHome()
        }

        remainingSeconds = savedInstanceState?.getInt(KEY_REMAINING, -1) ?: -1
        startCountdown()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_REMAINING, remainingSeconds)
    }

    private fun setupInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    private fun startCountdown() {
        countdownJob = lifecycleScope.launch {
            var remaining = if (remainingSeconds > 0) remainingSeconds else ConsciousPausePreferences.getDuration(this@ConsciousPauseActivity)
            while (remaining > 0) {
                remainingSeconds = remaining
                binding.tvCountdown.text = remaining.toString()

                val stateText = when (remaining % 4) {
                    0, 3 -> getString(R.string.pause_inhale)
                    else -> getString(R.string.pause_exhale)
                }
                binding.tvBreathingState.text = stateText

                delay(1000)
                remaining--
            }

            launchTargetApp()
        }
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

    override fun onDestroy() {
        countdownJob?.cancel()
        super.onDestroy()
    }
}
