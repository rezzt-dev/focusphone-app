package app.focusphone.ui.schedule

import android.os.SystemClock

/**
 * Short-lived unlock session for the schedule settings.
 *
 * The password is asked once per visit rather than on every edit: after a successful
 * verification the user can adjust several apps for [UNLOCK_DURATION_MS] without being
 * prompted again. The grant lives in memory only, so it disappears with the process and
 * can never be restored from disk.
 */
object ScheduleAccessGate {

    /** How long a successful password verification keeps the settings editable. */
    private const val UNLOCK_DURATION_MS = 5 * 60 * 1000L

    private var unlockedUntil = 0L

    /** Whether the schedule settings may currently be viewed and edited. */
    fun isUnlocked(): Boolean = SystemClock.elapsedRealtime() < unlockedUntil

    /** Starts (or extends) the unlock session. */
    fun unlock() {
        unlockedUntil = SystemClock.elapsedRealtime() + UNLOCK_DURATION_MS
    }

    /** Ends the unlock session immediately. */
    fun lock() {
        unlockedUntil = 0L
    }
}
