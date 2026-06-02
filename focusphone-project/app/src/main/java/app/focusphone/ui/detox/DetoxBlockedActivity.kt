package app.focusphone.ui.detox

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import app.focusphone.R
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.databinding.ActivityDetoxBlockedBinding
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity
import java.util.Locale

/**
 * Full-screen digital detox block activity.
 * Displays a live countdown timer and blocks app access strictly.
 */
class DetoxBlockedActivity : BaseActivity() {

    private lateinit var binding: ActivityDetoxBlockedBinding
    private var pulseAnimator: ObjectAnimator? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityDetoxBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        applyDetoxCircleTint()
        setupPulsingAnimation()
        startCountdownTimer()

        binding.btnBack.setOnClickListener {
            navigateHome()
        }
    }

    private fun applyDetoxCircleTint() {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        binding.viewDetoxCircle.backgroundTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
    }

    private fun setupInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
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
        val endTime = ProfilePreferences.getDetoxEndTime(this)
        val remainingMs = endTime - System.currentTimeMillis()
        if (remainingMs <= 0) {
            ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
            finish()
            return
        }

        binding.tvDetoxCountdown.text = formatTime(remainingMs)

        countDownTimer = object : CountDownTimer(remainingMs, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvDetoxCountdown.text = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                ProfilePreferences.setProfile(this@DetoxBlockedActivity, ProfilePreferences.PROFILE_NORMAL)
                finish()
            }
        }.start()
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

    private fun navigateHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        pulseAnimator?.cancel()
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
