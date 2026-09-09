package app.focusphone.ui.schedule

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import app.focusphone.R
import app.focusphone.data.preferences.AppSchedulePreferences
import app.focusphone.domain.model.AppInfo
import app.focusphone.domain.model.AppScheduleRule
import app.focusphone.domain.util.AppScheduleEvaluator
import app.focusphone.service.FocusAccessibilityService
import app.focusphone.ui.settings.SettingsSubActivity
import app.focusphone.ui.settings.addInfoRow
import app.focusphone.ui.settings.addSectionHeader
import app.focusphone.ui.settings.addValueRow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Password-protected screen listing every app with a time restriction.
 *
 * The screen renders nothing until the password check in [requestAccess] succeeds, so
 * the configured rules are not even visible to someone who cannot unlock them. The
 * first visit asks the user to create the password: schedules are only useful if the
 * person setting them cannot silently undo them a minute later.
 */
class AppScheduleActivity : SettingsSubActivity() {

    private var activeDialog: AlertDialog? = null
    private var installedApps: List<AppInfo> = emptyList()

    override fun getScreenTitleRes(): Int = R.string.schedule_title

    /** Content is rendered by [render] once the password gate has been passed. */
    override fun buildContent(container: LinearLayout) = Unit

    override fun onResume() {
        super.onResume()
        if (ScheduleAccessGate.isUnlocked()) render() else requestAccess()
    }

    override fun onPause() {
        super.onPause()
        activeDialog?.dismiss()
        activeDialog = null
    }

    // ── Password gate ──────────────────────────────────────────────

    private fun requestAccess() {
        val lockoutMs = AppSchedulePreferences.getLockoutRemainingMs(this)
        if (lockoutMs > 0) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(lockoutMs) + 1
            Toast.makeText(this, getString(R.string.schedule_locked_out, minutes), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (AppSchedulePreferences.hasPassword(this)) showVerifyDialog() else showCreatePasswordDialog()
    }

    private fun showVerifyDialog() {
        showPasswordDialog(
            titleRes = R.string.schedule_unlock_title,
            descRes = R.string.schedule_unlock_desc,
            requireConfirm = false,
            confirmLabelRes = R.string.detox_btn_unlock,
            onCancel = { finish() }
        ) { password, _ ->
            if (AppSchedulePreferences.verifyPassword(this, password)) {
                ScheduleAccessGate.unlock()
                render()
                null
            } else {
                onWrongPassword(closeScreenOnLockout = true)
            }
        }
    }

    private fun showCreatePasswordDialog(onDone: (() -> Unit)? = null) {
        showPasswordDialog(
            titleRes = R.string.schedule_create_password_title,
            descRes = R.string.schedule_create_password_desc,
            requireConfirm = true,
            confirmLabelRes = R.string.action_save,
            onCancel = { if (onDone == null) finish() }
        ) { password, confirmation ->
            when {
                password.length < AppSchedulePreferences.MIN_PASSWORD_LENGTH ->
                    getString(R.string.schedule_password_too_short, AppSchedulePreferences.MIN_PASSWORD_LENGTH)

                password != confirmation ->
                    getString(R.string.schedule_password_mismatch)

                else -> {
                    AppSchedulePreferences.setPassword(this, password)
                    ScheduleAccessGate.unlock()
                    render()
                    onDone?.invoke()
                    null
                }
            }
        }
    }

    /**
     * Shows the shared password dialog.
     *
     * [onSubmit] receives the entered values and returns an error message to display
     * inline, or null when the action succeeded and the dialog should close.
     */
    private fun showPasswordDialog(
        titleRes: Int,
        descRes: Int,
        requireConfirm: Boolean,
        confirmLabelRes: Int,
        onCancel: () -> Unit,
        onSubmit: (password: String, confirmation: String) -> String?
    ) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_password, null)
        applyDialogSurface(view)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()

        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            // Passwords are typed here: keep them out of screenshots and screen recordings.
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        view.findViewById<TextView>(R.id.tv_schedule_password_title).setText(titleRes)
        view.findViewById<TextView>(R.id.tv_schedule_password_desc).setText(descRes)
        val etPassword = view.findViewById<EditText>(R.id.et_schedule_password)
        val etConfirm = view.findViewById<EditText>(R.id.et_schedule_password_confirm)
        val tvError = view.findViewById<TextView>(R.id.tv_schedule_password_error)
        etConfirm.visibility = if (requireConfirm) View.VISIBLE else View.GONE

        val btnOk = view.findViewById<TextView>(R.id.btn_ok)
        btnOk.setText(confirmLabelRes)
        btnOk.setOnClickListener {
            val error = onSubmit(etPassword.text.toString(), etConfirm.text.toString())
            if (error == null) {
                dialog.dismiss()
            } else {
                tvError.text = error
                tvError.visibility = View.VISIBLE
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        activeDialog = dialog
        dialog.show()
        applyFontFamilyToView(view)
    }

    /**
     * Builds the inline error for a rejected password, or closes the dialog and reports
     * the cooldown once the attempt budget is spent.
     *
     * @return the message to show inline, or null when the dialog was handled already.
     */
    private fun onWrongPassword(closeScreenOnLockout: Boolean): String? {
        if (AppSchedulePreferences.isLockedOut(this)) {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(
                AppSchedulePreferences.getLockoutRemainingMs(this)
            ) + 1
            Toast.makeText(this, getString(R.string.schedule_locked_out, minutes), Toast.LENGTH_LONG).show()
            if (closeScreenOnLockout) finish()
            return null
        }
        return getString(
            R.string.schedule_wrong_password,
            AppSchedulePreferences.getRemainingAttempts(this)
        )
    }

    // ── Rendering ──────────────────────────────────────────────────

    private fun render() {
        val container = contentContainer
        container.removeAllViews()

        val rules = AppSchedulePreferences.getRules(this).sortedBy { labelFor(it.packageName) }

        container.addSectionHeader(R.string.schedule_section_apps)

        if (rules.isEmpty()) {
            container.addInfoRow(
                titleResId = R.string.schedule_empty_title,
                subtitleResId = R.string.schedule_empty_desc,
                iconResId = R.drawable.ic_settings_schedule,
                onClick = { showAppPicker() }
            )
        } else {
            rules.forEach { addRuleRow(container, it) }
        }

        container.addValueRow(
            titleResId = R.string.schedule_add_app,
            subtitleResId = R.string.schedule_add_app_desc,
            initialValue = null,
            iconResId = R.drawable.ic_add,
            onClick = { showAppPicker() }
        )

        // Daily limits are measured by the accessibility service; without it only the
        // time windows are enforced, so the user has to know it is off.
        if (rules.any { it.hasDailyLimit } && !FocusAccessibilityService.isEnabled(this)) {
            container.addView(inflater.inflate(R.layout.settings_divider, container, false))
            container.addSectionHeader(R.string.schedule_section_warning)
            container.addInfoRow(
                titleResId = R.string.schedule_accessibility_required,
                subtitleResId = R.string.schedule_accessibility_required_desc,
                iconResId = R.drawable.ic_settings_accessibility,
                onClick = { FocusAccessibilityService.openAccessibilitySettings(this) }
            )
        }

        container.addView(inflater.inflate(R.layout.settings_divider, container, false))
        container.addSectionHeader(R.string.schedule_section_protection)
        container.addValueRow(
            titleResId = R.string.schedule_password_row_title,
            subtitleResId = R.string.schedule_password_row_desc,
            initialValue = null,
            iconResId = R.drawable.ic_settings_permissions,
            onClick = { showPasswordOptions() }
        )

        applyFontFamilyToView(binding.root)
    }

    private fun addRuleRow(container: LinearLayout, rule: AppScheduleRule) {
        val row = inflater.inflate(R.layout.item_schedule_rule, container, false)
        row.findViewById<TextView>(R.id.tv_app_label).text = labelFor(rule.packageName)
        row.findViewById<TextView>(R.id.tv_rule_summary).text = summaryOf(rule)

        val usageView = row.findViewById<TextView>(R.id.tv_rule_usage)
        if (rule.hasDailyLimit) {
            val used = AppSchedulePreferences.getUsedMinutes(this, rule.packageName)
            usageView.text = getString(R.string.schedule_used_today, used, rule.dailyLimitMinutes)
            usageView.visibility = View.VISIBLE
        } else {
            usageView.visibility = View.GONE
        }

        row.setOnClickListener { openEditor(rule.packageName) }
        container.addView(row)
    }

    /** Human-readable one-line description of what a rule restricts. */
    private fun summaryOf(rule: AppScheduleRule): String {
        val parts = mutableListOf<String>()
        if (rule.windowEnabled) {
            parts += getString(
                R.string.schedule_summary_window,
                AppScheduleEvaluator.formatMinuteOfDay(rule.startMinute),
                AppScheduleEvaluator.formatMinuteOfDay(rule.endMinute)
            )
            if (rule.days.size < AppScheduleRule.ALL_DAYS.size) {
                parts += ScheduleDayLabels.summarize(rule.days)
            }
        }
        if (rule.hasDailyLimit) {
            parts += getString(R.string.schedule_summary_limit, rule.dailyLimitMinutes)
        }
        return parts.joinToString(" · ")
    }

    private fun labelFor(packageName: String): String {
        focusPhoneApp.container.customAppLabelsPreferences.getCustomLabel(packageName)?.let { return it }
        installedApps.firstOrNull { it.packageName == packageName }?.let { return it.label }
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString().lowercase()
        } catch (e: Exception) {
            packageName
        }
    }

    // ── Actions ────────────────────────────────────────────────────

    private fun openEditor(packageName: String) {
        startActivity(AppScheduleEditActivity.newIntent(this, packageName, labelFor(packageName)))
    }

    /** Loads the installed apps once and lets the user pick one to restrict. */
    private fun showAppPicker() {
        lifecycleScope.launch {
            if (installedApps.isEmpty()) {
                installedApps = withContext(Dispatchers.IO) {
                    focusPhoneApp.container.getInstalledAppsUseCase()
                }
            }

            val configured = AppSchedulePreferences.getRules(this@AppScheduleActivity)
                .map { it.packageName }
                .toSet()
            val candidates = installedApps
                .filter { it.packageName != this@AppScheduleActivity.packageName && it.packageName !in configured }
                .sortedBy { it.label }

            if (candidates.isEmpty()) {
                Toast.makeText(this@AppScheduleActivity, R.string.schedule_no_apps_left, Toast.LENGTH_SHORT).show()
                return@launch
            }

            activeDialog = MaterialAlertDialogBuilder(this@AppScheduleActivity)
                .setTitle(R.string.schedule_add_app)
                .setItems(candidates.map { it.label }.toTypedArray()) { _, which ->
                    val app = candidates[which]
                    startActivity(AppScheduleEditActivity.newIntent(this@AppScheduleActivity, app.packageName, app.label))
                }
                .create()
            activeDialog?.show()
            styleMaterialDialog(activeDialog)
        }
    }

    private fun showPasswordOptions() {
        val options = arrayOf(
            getString(R.string.schedule_password_change),
            getString(R.string.schedule_password_remove)
        )

        activeDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.schedule_password_row_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmCurrentPassword { showCreatePasswordDialog(onDone = {}) }
                    1 -> confirmCurrentPassword { removeProtection() }
                }
            }
            .create()
        activeDialog?.show()
        styleMaterialDialog(activeDialog)
    }

    /**
     * Asks for the current password before a protection change. The unlock session is
     * not enough here: changing or removing the lock is exactly the action the password
     * exists to guard.
     */
    private fun confirmCurrentPassword(onVerified: () -> Unit) {
        showPasswordDialog(
            titleRes = R.string.schedule_confirm_password_title,
            descRes = R.string.schedule_confirm_password_desc,
            requireConfirm = false,
            confirmLabelRes = R.string.detox_btn_unlock,
            onCancel = {}
        ) { password, _ ->
            if (AppSchedulePreferences.verifyPassword(this, password)) {
                onVerified()
                null
            } else {
                onWrongPassword(closeScreenOnLockout = false)
            }
        }
    }

    /** Clears the password and every rule, since unprotected schedules are pointless. */
    private fun removeProtection() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.schedule_password_remove)
            .setMessage(R.string.schedule_password_remove_confirm)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.schedule_action_remove) { _, _ ->
                AppSchedulePreferences.getRules(this).forEach {
                    AppSchedulePreferences.removeRule(this, it.packageName)
                }
                AppSchedulePreferences.setPassword(this, null)
                ScheduleAccessGate.lock()
                Toast.makeText(this, R.string.schedule_password_removed, Toast.LENGTH_SHORT).show()
                finish()
            }
            .create()
            .also { activeDialog = it; it.show(); styleMaterialDialog(it) }
    }

    companion object {
        /** Opens the schedule settings from anywhere in the app. */
        fun newIntent(context: android.content.Context): Intent =
            Intent(context, AppScheduleActivity::class.java)
    }
}
