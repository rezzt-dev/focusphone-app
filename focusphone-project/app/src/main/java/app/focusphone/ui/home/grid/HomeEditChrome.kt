package app.focusphone.ui.home.grid

import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import app.focusphone.R
import app.focusphone.ui.theme.FocusColors
import app.focusphone.ui.theme.FocusPalette

/**
 * The controls laid over the home grid in edit mode: a bottom bar (widgets, clock, grid,
 * done) and a small bar of actions that floats next to the selected block.
 */
class HomeEditChrome(parent: FrameLayout) {

    /** One action of the selected block. */
    data class Action(val label: String, val run: () -> Unit)

    val root: FrameLayout = LayoutInflater.from(parent.context)
        .inflate(R.layout.layout_home_edit_chrome, parent, false) as FrameLayout

    private val itemBar: LinearLayout = root.findViewById(R.id.edit_item_bar)
    private val itemTitle: TextView = root.findViewById(R.id.tv_item_title)
    private val itemPrimary: TextView = root.findViewById(R.id.btn_item_primary)
    private val itemRemove: TextView = root.findViewById(R.id.btn_item_remove)
    private val bottom: LinearLayout = root.findViewById(R.id.edit_bottom)
    private val hint: TextView = root.findViewById(R.id.tv_edit_hint)
    private val bar: LinearLayout = root.findViewById(R.id.edit_bar)
    val widgetsButton: TextView = root.findViewById(R.id.btn_edit_widgets)
    val clockButton: TextView = root.findViewById(R.id.btn_edit_clock)
    val gridButton: TextView = root.findViewById(R.id.btn_edit_grid)
    val doneButton: TextView = root.findViewById(R.id.btn_edit_done)

    private val density = root.resources.displayMetrics.density
    private val fadeDuration = root.resources.getInteger(R.integer.fp_grid_edit_duration).toLong()
    private val bottomPadding = bottom.paddingBottom

    init {
        // The bars draw their own rounded surface; the screen-wide recolouring must not
        // flatten them into plain rectangles.
        bar.setTag(R.id.tag_skip_recolor, true)
        itemBar.setTag(R.id.tag_skip_recolor, true)
        // The bottom bar shares the screen's width equally; a label that does not fit its
        // share (narrow screen, large font, a long translation) gets smaller, never cut.
        listOf(widgetsButton, clockButton, gridButton, doneButton).forEach {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                it,
                MIN_BUTTON_TEXT_SP,
                MAX_BUTTON_TEXT_SP,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
        parent.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    /** Height the bottom controls take from the bottom of the screen, once laid out. */
    val bottomHeight: Int get() = bottom.height

    fun applyPalette(colors: FocusPalette) {
        bar.background = panel(colors)
        itemBar.background = panel(colors)
        hint.setTextColor(colors.textSecondary)
        itemTitle.setTextColor(colors.textSecondary)
        listOf(widgetsButton, clockButton, gridButton, itemPrimary, itemRemove).forEach { it.setTextColor(colors.textPrimary) }
        doneButton.setTextColor(colors.accent)
    }

    /** Softly rounded corners, like the blocks of the grid, rather than pill-shaped ends. */
    private fun panel(colors: FocusPalette) = GradientDrawable().apply {
        cornerRadius = root.resources.getDimension(R.dimen.home_grid_corner)
        setColor(colors.surface)
        setStroke(density.toInt().coerceAtLeast(1), FocusColors.withAlpha(colors.textPrimary, PANEL_STROKE_ALPHA))
    }

    /** Space taken by the status bar, which the floating actions must stay below. */
    private var topInset = 0

    fun setSystemInsets(top: Int, bottom: Int) {
        topInset = top
        this.bottom.updatePadding(bottom = bottomPadding + bottom)
    }

    fun show(visible: Boolean) {
        root.animate().cancel()
        if (visible) {
            root.alpha = 0f
            root.isVisible = true
            root.animate().alpha(1f).setDuration(fadeDuration).setInterpolator(DecelerateInterpolator()).start()
        } else {
            itemBar.isVisible = false
            root.animate().alpha(0f).setDuration(fadeDuration).withEndAction { root.isVisible = false }.start()
        }
    }

    // ── Block actions ──────────────────────────────────────────────

    /**
     * Shows the actions of the selected block next to [anchor] (in this chrome's coordinates):
     * above it when there is room, otherwise below it, otherwise over its top edge.
     */
    fun showItemBar(title: String, primary: Action?, remove: Action?, anchor: RectF) {
        itemTitle.text = title
        bind(itemPrimary, primary)
        bind(itemRemove, remove)
        itemBar.isVisible = true
        itemBar.post { placeItemBar(anchor) }
    }

    private fun bind(button: TextView, action: Action?) {
        button.isVisible = action != null
        action ?: return
        button.text = action.label
        button.setOnClickListener { action.run() }
    }

    private fun placeItemBar(anchor: RectF) {
        val gap = ITEM_BAR_GAP_DP * density
        val width = itemBar.width.toFloat()
        val height = itemBar.height.toFloat()
        val maxX = (root.width - width).coerceAtLeast(0f)
        val bottomLimit = root.height - bottomHeight.toFloat()
        val y = when {
            anchor.top - gap - height >= topInset -> anchor.top - gap - height
            anchor.bottom + gap + height <= bottomLimit -> anchor.bottom + gap
            else -> anchor.top + gap
        }
        itemBar.x = (anchor.centerX() - width / 2f).coerceIn(0f, maxX)
        itemBar.y = y
    }

    fun hideItemBar() {
        itemBar.isVisible = false
    }

    private companion object {
        const val PANEL_STROKE_ALPHA = 0.12f
        const val MIN_BUTTON_TEXT_SP = 10
        const val MAX_BUTTON_TEXT_SP = 14
        const val ITEM_BAR_GAP_DP = 10f
    }
}
