package app.focusphone.ui.util

import android.animation.StateListAnimator
import android.view.View
import app.focusphone.R

/*
 * Rows answer a press through `@animator/fp_press_feedback`, which animates their alpha
 * and scale. A drag animates the very same properties, and the two would fight over every
 * frame — the press animator even snaps the row back to full opacity the moment the list
 * cancels the touch to start dragging. While a gesture owns the row, the press animator
 * is parked on the view and handed back once the row has settled.
 */

/** Parks this row's press animator so a drag can drive its alpha and scale alone. */
fun View.suspendPressFeedback() {
    val animator = stateListAnimator ?: return
    setTag(R.id.tag_press_animator, animator)
    stateListAnimator = null
    isPressed = false
}

/** Gives back the press animator parked by [suspendPressFeedback], if any. */
fun View.resumePressFeedback() {
    val animator = getTag(R.id.tag_press_animator) as? StateListAnimator ?: return
    setTag(R.id.tag_press_animator, null)
    stateListAnimator = animator
}
