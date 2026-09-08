package app.focusphone.data.preferences

import android.content.Context

/**
 * Synchronous preferences for the interactive tutorial state.
 *
 * The tutorial is a contextual, step-by-step guide that highlights real UI elements
 * in [app.focusphone.ui.home.HomeActivity]. Progress is persisted so users can resume
 * if they leave the app mid-tutorial.
 */
object TutorialPreferences {

    private const val PREF_FILE = "focusphone_tutorial"
    private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
    private const val KEY_CURRENT_STEP = "tutorial_current_step"
    private const val KEY_ONBOARDING_FLOW_COMPLETED = "onboarding_flow_completed"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** Returns true if the user has fully completed the interactive tutorial. */
    fun isTutorialComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TUTORIAL_COMPLETED, false)

    /** Marks the entire tutorial as completed. */
    fun markTutorialComplete(context: Context) =
        prefs(context).edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()

    /** Resets the tutorial so it can be shown again from the beginning. */
    fun resetTutorial(context: Context) =
        prefs(context).edit()
            .putBoolean(KEY_TUTORIAL_COMPLETED, false)
            .putInt(KEY_CURRENT_STEP, 0)
            .apply()

    /** The current tutorial step index (0-based). Defaults to 0. */
    fun getCurrentStep(context: Context): Int =
        prefs(context).getInt(KEY_CURRENT_STEP, 0)

    /** Advances to the next step. */
    fun setCurrentStep(context: Context, step: Int) =
        prefs(context).edit().putInt(KEY_CURRENT_STEP, step).apply()

    /**
     * Whether the full onboarding flow (the 7-step carousel) has been completed.
     * This is separate from [ThemePreferences.isOnboardingComplete] which only tracks
     * the legacy 3-step flow. Once the new flow is finished, this flag is set.
     */
    fun isOnboardingFlowCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_FLOW_COMPLETED, false)

    fun setOnboardingFlowCompleted(context: Context, completed: Boolean = true) =
        prefs(context).edit().putBoolean(KEY_ONBOARDING_FLOW_COMPLETED, completed).apply()

    /** Reset all tutorial and onboarding-flow state. Used when the user restarts onboarding. */
    fun clearAll(context: Context) =
        prefs(context).edit().clear().apply()
}
