package app.focusphone.ui.detox

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.databinding.ActivityDetoxBlockedBinding
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity
import app.focusphone.ui.widget.library.WidgetTrigger
import app.focusphone.ui.widget.library.WidgetUpdater
import java.util.Locale

/**
 * Full-screen digital detox block activity.
 * Displays a live countdown timer and blocks app access strictly.
 * Offers a direct unlock path via password verification.
 */
class DetoxBlockedActivity : BaseActivity() {

    private lateinit var binding: ActivityDetoxBlockedBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityDetoxBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        startCountdownTimer()

        binding.btnBack.setOnClickListener {
            navigateHome()
        }

        binding.btnUnlock.setOnClickListener {
            showDetoxUnlockDialog {
                ProfilePreferences.endDetoxSession(this)
                WidgetUpdater.refresh(this, WidgetTrigger.PROFILE)
                navigateHome()
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateHome()
                }
            }
        )
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun startCountdownTimer() {
        countDownTimer?.cancel()
        val remainingMs = ProfilePreferences.getDetoxEndTime(this) - System.currentTimeMillis()
        if (remainingMs <= 0) {
            ProfilePreferences.expireDetoxIfNeeded(this)
            finish()
            return
        }

        binding.tvDetoxCountdown.text = formatTime(remainingMs)

        countDownTimer = object : CountDownTimer(remainingMs, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvDetoxCountdown.text = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                // The timer runs on the elapsed-time clock while the session end is wall
                // time; if the two drifted, keep counting instead of ending early.
                ProfilePreferences.expireDetoxIfNeeded(this@DetoxBlockedActivity)
                if (ProfilePreferences.isDetoxActive(this@DetoxBlockedActivity)) {
                    startCountdownTimer()
                } else {
                    finish()
                }
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
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
