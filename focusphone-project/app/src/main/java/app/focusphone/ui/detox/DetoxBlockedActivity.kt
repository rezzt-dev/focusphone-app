package app.focusphone.ui.detox

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.focusphone.R
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.databinding.ActivityDetoxBlockedBinding
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Full-screen digital detox block activity.
 * Displays a live countdown timer and blocks app access strictly.
 */
class DetoxBlockedActivity : BaseActivity() {

    private lateinit var binding: ActivityDetoxBlockedBinding
    private var pulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetoxBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPulsingAnimation()
        startCountdownTimer()
    }

    private fun setupPulsingAnimation() {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.03f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.03f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.viewDetoxCircle, scaleX, scaleY).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun startCountdownTimer() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val endTime = ProfilePreferences.getDetoxEndTime(this@DetoxBlockedActivity)
                    val remainingMs = endTime - System.currentTimeMillis()

                    if (remainingMs <= 0) {
                        ProfilePreferences.setProfile(this@DetoxBlockedActivity, ProfilePreferences.PROFILE_NORMAL)
                        finish()
                        break
                    }

                    binding.tvDetoxCountdown.text = formatTime(remainingMs)
                    delay(500)
                }
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        pulseAnimator?.cancel()
        super.onDestroy()
    }
}
