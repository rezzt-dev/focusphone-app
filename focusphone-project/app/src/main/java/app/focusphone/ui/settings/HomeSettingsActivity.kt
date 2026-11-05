package app.focusphone.ui.settings

import android.content.Intent
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import app.focusphone.R
import app.focusphone.data.preferences.FontPreferences
import app.focusphone.data.preferences.HomeLayoutPreferences
import app.focusphone.data.preferences.WidgetPreferences
import app.focusphone.databinding.ItemSettingsClockTypeBinding
import app.focusphone.databinding.SettingsSliderRowBinding
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.util.currentWindowWidthPx
import app.focusphone.ui.widget.AppWidgetHostManager
import app.focusphone.ui.widget.HomeWidgetActions
import com.google.android.material.slider.Slider

/**
 * Home screen settings: clock type, clock size, app spacing, widget and layout.
 */
class HomeSettingsActivity : SettingsSubActivity() {

    private var widgetValueView: TextView? = null
    private var clockSizeSlider: Slider? = null
    private var appSpacingSlider: Slider? = null

    private val widgetActions = HomeWidgetActions(
        activity = this,
        showDialog = { showStyledDialog(it) },
        onWidgetChanged = ::updateWidgetStatus
    )

    override fun getScreenTitleRes(): Int = R.string.home_settings_title

    override fun buildContent(container: LinearLayout) {
        // ── Clock ────────────────────────────────────────────────────
        container.settingsSection(R.string.settings_section_clock) {
            val clockTypeContainer = LinearLayout(this@HomeSettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(20), dp(4), dp(20), dp(4))
            }
            setupClockTypeSelector(clockTypeContainer)
            addView(clockTypeContainer)

            val currentClockSize = FontPreferences.getClockSizePercent(this@HomeSettingsActivity)
            val clockSizeBinding = SettingsSliderRowBinding.inflate(inflater, this, false)
            clockSizeBinding.ivIcon.setImageResource(R.drawable.ic_settings_clock)
            clockSizeBinding.ivIcon.visibility = View.VISIBLE
            clockSizeBinding.tvLabel.setText(R.string.settings_clock_size)
            clockSizeBinding.slider.apply {
                valueFrom = 0f
                valueTo = 100f
                stepSize = 1f
                value = currentClockSize.toFloat().coerceIn(0f, 100f)
                persistOnStop { value ->
                    FontPreferences.setClockSizePercent(this@HomeSettingsActivity, value.toInt())
                }
            }
            clockSizeSlider = clockSizeBinding.slider
            addView(clockSizeBinding.root)
        }

        // ── Layout & Spacing ─────────────────────────────────────────
        container.settingsSection(R.string.settings_section_launcher) {
            val currentSpacing = HomeLayoutPreferences.getAppSpacingDp(this@HomeSettingsActivity)
            val spacingBinding = SettingsSliderRowBinding.inflate(inflater, this, false)
            spacingBinding.ivIcon.setImageResource(R.drawable.ic_settings_spacing)
            spacingBinding.ivIcon.visibility = View.VISIBLE
            spacingBinding.tvLabel.text =
                getString(R.string.settings_app_spacing_dp, currentSpacing)
            spacingBinding.slider.apply {
                valueFrom = 0f
                valueTo = 40f
                stepSize = 1f
                value = currentSpacing.toFloat().coerceIn(0f, 40f)
                addOnChangeListener { _, value, _ ->
                    val spacing = value.toInt()
                    spacingBinding.tvLabel.text =
                        getString(R.string.settings_app_spacing_dp, spacing)
                }
                persistOnStop { value ->
                    HomeLayoutPreferences.setAppSpacingDp(this@HomeSettingsActivity, value.toInt())
                }
            }
            appSpacingSlider = spacingBinding.slider
            addView(spacingBinding.root)

            widgetValueView = addValueRow(
                titleResId = R.string.settings_widget_title,
                subtitleResId = R.string.settings_widget_settings_desc,
                initialValue = getWidgetStatusText(),
                iconResId = R.drawable.ic_settings_widget,
                onClick = { widgetActions.showOptions() }
            )

            addCategoryRow(
                iconResId = R.drawable.ic_settings_layout,
                titleResId = R.string.home_layout_settings_title,
                subtitleResId = R.string.home_layout_settings_desc,
                onClick = {
                    startActivity(Intent(this@HomeSettingsActivity, HomeLayoutActivity::class.java))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateWidgetStatus()
    }

    private fun setupClockTypeSelector(container: LinearLayout) {
        val current = FontPreferences.getClockType(this)
        val tiles = mutableListOf<View>()
        val previewWidthPx = clockTilePreviewWidthPx()

        ClockType.entries.forEach { type ->
            val item = ItemSettingsClockTypeBinding.inflate(inflater, container, false)
            item.tvPreview.text = when (type) {
                ClockType.WORDS -> getString(R.string.clock_preview_words)
                ClockType.HIDDEN -> getString(R.string.settings_clock_hidden)
                else -> type.previewLabel
            }
            val labelRes = when (type) {
                ClockType.DIGITAL -> R.string.settings_clock_digital
                ClockType.STACKED -> R.string.settings_clock_stacked
                ClockType.WORDS -> R.string.settings_clock_words
                ClockType.ANALOG -> R.string.settings_clock_analog
                ClockType.HIDDEN -> R.string.settings_clock_hidden
            }
            item.tvLabel.text = getString(labelRes).lowercase()
            fitPreviewText(item.tvPreview, previewWidthPx)
            item.root.alpha = if (type == current) 1f else 0.6f
            item.root.setOnClickListener {
                FontPreferences.setClockType(this, type)
                tiles.forEach { it.alpha = 0.6f }
                item.root.alpha = 1f
            }
            container.addView(item.root)
            tiles.add(item.root)
        }
    }

    /**
     * Usable width of one clock-type preview, derived from the row's own geometry.
     *
     * Computed rather than measured so the text size is final on the very first frame, with
     * no layout pass that resizes the tiles under the user.
     */
    private fun clockTilePreviewWidthPx(): Int {
        val rowWidth = currentWindowWidthPx() - 2 * dp(CLOCK_ROW_PADDING_DP)
        val tileWidth = rowWidth / ClockType.entries.size
        return tileWidth - dp(CLOCK_TILE_MARGIN_DP) - 2 * dp(CLOCK_TILE_PADDING_DP)
    }

    /**
     * Shrinks [preview] until its longest word fits on one line.
     *
     * `autoSizeTextType` cannot express this: it stops at the first size that fits the
     * available height, and a size that only fits because a word was split in half still
     * counts — which is how the Spanish preview rendered as "nueve c / uarenta / y uno" and
     * the German one ("einundvierzig") fared worse.
     */
    private fun fitPreviewText(preview: TextView, availableWidthPx: Int) {
        if (availableWidthPx <= 0) return
        val longestWord = preview.text.split(' ', '\n').maxByOrNull { it.length }.orEmpty()
        if (longestWord.isEmpty()) return

        // A little slack below the measured width: letter spacing, hinting and integer
        // rounding at layout time all cost a pixel or two, and landing exactly on the limit
        // is what still let "cuarenta" spill onto a second line.
        val budget = availableWidthPx * WIDTH_SAFETY_MARGIN
        val paint = TextPaint(preview.paint)
        val minPx = spToPx(CLOCK_PREVIEW_MIN_SP)
        var sizePx = spToPx(CLOCK_PREVIEW_MAX_SP)
        while (sizePx > minPx) {
            paint.textSize = sizePx
            if (paint.measureText(longestWord) <= budget) break
            sizePx -= 1f
        }
        preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx.coerceAtLeast(minPx))
    }

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

    private fun updateWidgetStatus() {
        widgetValueView?.text = getWidgetStatusText()
    }

    /** The current widget's name, so the row tells which widget is on the home screen. */
    private fun getWidgetStatusText(): String {
        if (!WidgetPreferences.hasWidget(this)) return getString(R.string.settings_widget_none).lowercase()
        val info = AppWidgetHostManager.getInstance(this).homeWidgetInfo(this)
            ?: return getString(R.string.settings_widget_none).lowercase()
        return info.loadLabel(packageManager)?.toString()?.lowercase()
            ?: getString(R.string.settings_widget_title)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        /** Mirrors the padding applied to the clock-type row and to `item_settings_clock_type`. */
        const val CLOCK_ROW_PADDING_DP = 20
        const val CLOCK_TILE_MARGIN_DP = 6
        const val CLOCK_TILE_PADDING_DP = 6

        const val CLOCK_PREVIEW_MAX_SP = 18f
        const val CLOCK_PREVIEW_MIN_SP = 7f
        const val WIDTH_SAFETY_MARGIN = 0.92f
    }
}
