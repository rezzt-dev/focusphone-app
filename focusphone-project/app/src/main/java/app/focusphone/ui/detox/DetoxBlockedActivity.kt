package app.focusphone.ui.detox

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focusphone.R
import app.focusphone.data.preferences.ProfilePreferences
import app.focusphone.databinding.ActivityDetoxBlockedBinding
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity
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
            showUnlockDialog()
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateHome()
            }
        })
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
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

    private fun showUnlockDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_detox_password_verify, null)
        applyDialogSurface(view)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val etPassword = view.findViewById<EditText>(R.id.et_detox_password_verify)

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            if (ProfilePreferences.isPasswordLockedOut(this)) {
                Toast.makeText(this, getString(R.string.detox_force_disabled), Toast.LENGTH_LONG).show()
                ProfilePreferences.setDetoxPassword(this, null)
                ProfilePreferences.setDetoxEndTime(this, 0L)
                ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
                ProfilePreferences.clearFailedAttempts(this)
                dialog.dismiss()
                navigateHome()
                return@setOnClickListener
            }

            val entered = etPassword.text.toString()
            if (ProfilePreferences.verifyDetoxPassword(this, entered)) {
                ProfilePreferences.clearFailedAttempts(this)
                ProfilePreferences.setDetoxPassword(this, null)
                ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
                dialog.dismiss()
                navigateHome()
            } else {
                val attempts = ProfilePreferences.getFailedAttempts(this)
                if (attempts >= 4) {
                    ProfilePreferences.setDetoxPassword(this, null)
                    ProfilePreferences.setDetoxEndTime(this, 0L)
                    ProfilePreferences.setProfile(this, ProfilePreferences.PROFILE_NORMAL)
                    ProfilePreferences.clearFailedAttempts(this)
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        getString(R.string.detox_force_disabled),
                        Toast.LENGTH_LONG
                    ).show()
                    navigateHome()
                } else {
                    Toast.makeText(this, getString(R.string.error_incorrect_password), Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
