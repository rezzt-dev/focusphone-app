package app.focusphone.ui.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focusphone.R
import app.focusphone.databinding.ActivityScheduleBlockedBinding
import app.focusphone.domain.model.ScheduleBlockReason
import app.focusphone.domain.util.AppScheduleEvaluator
import app.focusphone.ui.base.BaseActivity
import app.focusphone.ui.home.HomeActivity

/**
 * Full-screen block page shown when an app is opened outside its allowed time window
 * or after its daily allowance has been used up.
 *
 * Unlike [app.focusphone.ui.detox.DetoxBlockedActivity] there is no unlock button:
 * schedules are a standing configuration, and the only way to change them is the
 * password-protected settings screen. Offering an unlock here would make the lock a
 * single tap away from the very moment the user is trying to bypass it.
 */
class ScheduleBlockedActivity : BaseActivity() {

    private lateinit var binding: ActivityScheduleBlockedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()

        binding.tvScheduleApp.text = resolveAppLabel()
        renderReason()

        binding.btnBack.setOnClickListener { navigateHome() }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = navigateHome()
            }
        )

        applyFontFamilyToView(binding.root)
    }

    /** The activity is singleTop: a block for another app must replace the content. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        binding.tvScheduleApp.text = resolveAppLabel()
        renderReason()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    /** Falls back to the package name when the app label cannot be resolved. */
    private fun resolveAppLabel(): String {
        intent.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() }?.let { return it }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return ""
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString().lowercase()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun renderReason() {
        when (intent.getStringExtra(EXTRA_REASON)) {
            REASON_DAILY_LIMIT -> {
                val used = intent.getIntExtra(EXTRA_USED_MINUTES, 0)
                val limit = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 0)
                binding.tvScheduleTitle.setText(R.string.schedule_blocked_limit_title)
                binding.tvScheduleDesc.text = resources.getQuantityString(
                    R.plurals.schedule_blocked_limit_desc, limit, used, limit
                )
            }

            else -> {
                val start = intent.getIntExtra(EXTRA_START_MINUTE, 0)
                val end = intent.getIntExtra(EXTRA_END_MINUTE, 0)
                binding.tvScheduleTitle.setText(R.string.schedule_blocked_window_title)
                binding.tvScheduleDesc.text = getString(
                    R.string.schedule_blocked_window_desc,
                    AppScheduleEvaluator.formatMinuteOfDay(start),
                    AppScheduleEvaluator.formatMinuteOfDay(end)
                )
            }
        }
    }

    private fun navigateHome() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    companion object {

        private const val EXTRA_PACKAGE = "schedule_package"
        private const val EXTRA_LABEL = "schedule_label"
        private const val EXTRA_REASON = "schedule_reason"
        private const val EXTRA_START_MINUTE = "schedule_start_minute"
        private const val EXTRA_END_MINUTE = "schedule_end_minute"
        private const val EXTRA_USED_MINUTES = "schedule_used_minutes"
        private const val EXTRA_LIMIT_MINUTES = "schedule_limit_minutes"

        private const val REASON_WINDOW = "outside_window"
        private const val REASON_DAILY_LIMIT = "daily_limit"

        /**
         * Builds the intent that shows why [packageName] is blocked.
         * [label] may be null, in which case the label is resolved from the package.
         */
        fun newIntent(
            context: Context,
            packageName: String,
            label: String?,
            reason: ScheduleBlockReason
        ): Intent = Intent(context, ScheduleBlockedActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_LABEL, label)
            when (reason) {
                is ScheduleBlockReason.OutsideWindow -> {
                    putExtra(EXTRA_REASON, REASON_WINDOW)
                    putExtra(EXTRA_START_MINUTE, reason.startMinute)
                    putExtra(EXTRA_END_MINUTE, reason.endMinute)
                }

                is ScheduleBlockReason.DailyLimitReached -> {
                    putExtra(EXTRA_REASON, REASON_DAILY_LIMIT)
                    putExtra(EXTRA_USED_MINUTES, reason.usedMinutes)
                    putExtra(EXTRA_LIMIT_MINUTES, reason.limitMinutes)
                }
            }
        }
    }
}
