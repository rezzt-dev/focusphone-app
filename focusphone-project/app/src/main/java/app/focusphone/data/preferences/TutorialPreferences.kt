package app.focusphone.data.preferences

import androidx.core.content.edit
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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /** Returns true if the user has fully completed the interactive tutorial. */
    fun isTutorialComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TUTORIAL_COMPLETED, false)

    /** Marks the entire tutorial as completed. */
    fun markTutorialComplete(context: Context) =
        prefs(context).edit { putBoolean(KEY_TUTORIAL_COMPLETED, true) }

    /** Resets the tutorial so it can be shown again from the beginning. */
    fun resetTutorial(context: Context) =
        prefs(context).edit {
            putBoolean(KEY_TUTORIAL_COMPLETED, false)
            putInt(KEY_CURRENT_STEP, 0)
        }

    /** The current tutorial step index (0-based). Defaults to 0. */
    fun getCurrentStep(context: Context): Int =
        prefs(context).getInt(KEY_CURRENT_STEP, 0)

    /** Advances to the next step. */
    fun setCurrentStep(context: Context, step: Int) =
        prefs(context).edit { putInt(KEY_CURRENT_STEP, step) }

    /** Resets the contextual tutorial state when onboarding is restarted. */
    fun clearAll(context: Context) =
        prefs(context).edit { clear() }
}
