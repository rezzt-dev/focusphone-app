package app.focusphone.ui.pause

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.focusphone.R
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.data.preferences.ConsciousPausePreferences
import app.focusphone.databinding.ActivityConsciousPauseBinding
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Conscious pause: one slow breath between the impulse and the app.
 *
 * The pause is a single breath stretched over the duration the user chose — breathe in
 * for the first half, out for the second — drawn by [BreathingOrbView]. A numeric
 * countdown alone used to tick through "inhale… exhale…" by the remainder of the seconds
 * left, which started a four-second pause on an exhale.
 *
 * Time is measured with the real clock on every frame and only advances while the screen
 * is in the foreground, so leaving it pauses the breath instead of letting it finish in
 * the background, where Android 10+ silently refuses to start the target app.
 */
class ConsciousPauseActivity : BaseActivity() {

    private lateinit var binding: ActivityConsciousPauseBinding

    private var totalMs = 0L
    private var elapsedMs = 0L
    private var renderedSeconds = -1
    private var renderedInhale: Boolean? = null
    private var launched = false

    /** The orb only breathes when the system allows motion; the countdown runs regardless. */
    private val motionEnabled: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsciousPauseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()

        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: getString(R.string.app_unknown)
        binding.tvMindfulQuestion.text = getString(R.string.pause_mindful_question, appLabel.lowercase(Locale.getDefault()))
        renderAllowanceLeft(intent.getStringExtra(EXTRA_PACKAGE_NAME))

        val colors = effectivePalette
        binding.orbBreath.setColors(accent = colors.textPrimary, track = colors.divider)
        binding.btnBack.setOnClickListener { navigateHome() }

        totalMs = ConsciousPausePreferences.getDuration(this) * MS_PER_SECOND
        elapsedMs = savedInstanceState?.getLong(KEY_ELAPSED_MS, 0L)?.coerceIn(0L, totalMs) ?: 0L
        render()
        runBreath()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ELAPSED_MS, elapsedMs)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    // ── Breath ─────────────────────────────────────────────────────

    private fun runBreath() {
        lifecycleScope.launch {
            // Cancelled when the screen leaves the foreground and resumed from the time
            // already breathed when it comes back.
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var lastFrame = SystemClock.uptimeMillis()
                while (elapsedMs < totalMs) {
                    awaitFrame()
                    val now = SystemClock.uptimeMillis()
                    elapsedMs = (elapsedMs + (now - lastFrame)).coerceAtMost(totalMs)
                    lastFrame = now
                    render()
                }
                launchTargetApp()
            }
        }
    }

    private fun render() {
        val fraction = if (totalMs > 0) elapsedMs.toFloat() / totalMs else 1f
        val inhale = fraction < HALF

        val secondsLeft = ((totalMs - elapsedMs + MS_PER_SECOND - 1) / MS_PER_SECOND).toInt()
        if (secondsLeft != renderedSeconds) {
            renderedSeconds = secondsLeft
            binding.tvCountdown.text = String.format(Locale.getDefault(), "%d", secondsLeft.coerceAtLeast(1))
        }

        if (inhale != renderedInhale) {
            // A soft tick when the breath turns lets the user follow it with eyes closed.
            if (renderedInhale != null) binding.orbBreath.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            renderedInhale = inhale
            binding.tvBreathingState.setText(if (inhale) R.string.pause_inhale else R.string.pause_exhale)
        }

        binding.orbBreath.progress = fraction
        binding.orbBreath.breath = if (motionEnabled) breathAt(fraction) else STILL_BREATH
    }

    /** Lung volume along a single breath: up for the first half, down for the second. */
    private fun breathAt(fraction: Float): Float {
        val phase = if (fraction < HALF) fraction / HALF else (1f - fraction) / HALF
        return BREATH_EASING.getInterpolation(phase.coerceIn(0f, 1f))
    }

    // ── Daily allowance ────────────────────────────────────────────

    /**
     * Reminds the user how much of a daily limit is left. Only apps that already carry a
     * limit have usage data at all — nothing is measured for anyone else — so for every
     * other app the line simply stays hidden.
     */
    private fun renderAllowanceLeft(packageName: String?) {
        val rule = packageName?.let { AppSchedulePreferences.getRule(this, it) }
        val secondsLeft = rule?.let { AppSchedulePreferences.getRemainingLimitSeconds(this, it) }
        if (secondsLeft == null) {
            binding.tvAllowanceLeft.visibility = View.GONE
            return
        }
        val minutes = secondsLeft / SECONDS_PER_MINUTE
        binding.tvAllowanceLeft.text = if (minutes < 1) {
            getString(R.string.pause_last_minute_today)
        } else {
            resources.getQuantityString(R.plurals.pause_minutes_left_today, minutes, minutes)
        }
        binding.tvAllowanceLeft.visibility = View.VISIBLE
    }

    // ── Navigation ─────────────────────────────────────────────────

    private fun launchTargetApp() {
        if (launched) return
        launched = true
        val targetIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TARGET_INTENT)
        }

        if (targetIntent != null) {
            try {
                startActivity(targetIntent)
            } catch (_: Exception) {
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
        private const val EXTRA_TARGET_INTENT = "target_intent"
        private const val EXTRA_APP_LABEL = "app_label"
        private const val EXTRA_PACKAGE_NAME = "package_name"
        private const val KEY_ELAPSED_MS = "elapsed_ms"

        private const val MS_PER_SECOND = 1000L
        private const val SECONDS_PER_MINUTE = 60
        private const val HALF = 0.5f
        private const val STILL_BREATH = 0.6f

        /** Slow in and out of every breath, like a real one. */
        private val BREATH_EASING = PathInterpolator(0.45f, 0f, 0.55f, 1f)

        /** Intent for a pause in front of [targetIntent], the launch intent of [packageName]. */
        fun newIntent(context: Context, targetIntent: Intent, label: String, packageName: String): Intent =
            Intent(context, ConsciousPauseActivity::class.java).apply {
                putExtra(EXTRA_TARGET_INTENT, targetIntent)
                putExtra(EXTRA_APP_LABEL, label)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
    }
}
