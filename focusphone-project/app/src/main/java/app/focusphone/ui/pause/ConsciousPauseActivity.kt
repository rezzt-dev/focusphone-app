package app.focusphone.ui.pause

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import app.focusphone.R
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.databinding.ActivityConsciousPauseBinding
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConsciousPauseActivity : BaseActivity() {

    private lateinit var binding: ActivityConsciousPauseBinding
    private var countdownJob: Job? = null
    private var scaleAnimator: AnimatorSet? = null
    private var remainingSeconds: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsciousPauseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()

        val appLabel = intent.getStringExtra("app_label") ?: "app"
        binding.tvMindfulQuestion.text = getString(R.string.pause_mindful_question, appLabel.lowercase())

        binding.btnCancel.setOnClickListener {
            finish()
        }

        startBreathingAnimation()
        remainingSeconds = savedInstanceState?.getInt(KEY_REMAINING, -1) ?: -1
        startCountdown()
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

    private fun startBreathingAnimation() {
        val animX = ObjectAnimator.ofFloat(binding.viewBreathingCircle, "scaleX", 0.9f, 1.15f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val animY = ObjectAnimator.ofFloat(binding.viewBreathingCircle, "scaleY", 0.9f, 1.15f).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }

        scaleAnimator = AnimatorSet().apply {
            playTogether(animX, animY)
            start()
        }
    }

    private fun startCountdown() {
        countdownJob = lifecycleScope.launch {
            var remaining = if (remainingSeconds > 0) remainingSeconds else ConsciousPausePreferences.getDuration(this@ConsciousPauseActivity)
            while (remaining > 0) {
                remainingSeconds = remaining
                binding.tvCountdown.text = remaining.toString()

                // Breathe-synchronized visual guidance (4-second total cycle: 2s inhale, 2s exhale)
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    companion object {
        private const val KEY_REMAINING = "remaining_seconds"
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
        scaleAnimator?.cancel()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Pressing back cancels the launch and goes back to home screen
        super.onBackPressed()
    }
}
