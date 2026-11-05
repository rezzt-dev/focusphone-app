package app.focusphone.ui.widget.library

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.updatePadding
import app.focusphone.R
import app.focusphone.data.preferences.LibraryWidgetPreferences
import app.focusphone.ui.settings.SettingsSubActivity
import app.focusphone.ui.settings.addSectionHeader

/**
 * Writes the line every intention widget shows. Saved when the user presses done or leaves
 * the screen, so there is no save button to forget.
 */
class IntentionConfigActivity : SettingsSubActivity() {

    private lateinit var field: EditText
    private var saved: String = ""

    override fun getScreenTitleRes(): Int = R.string.wg_intention_config_title

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
    }

    override fun buildContent(container: LinearLayout) {
        saved = LibraryWidgetPreferences.getIntention(this)
        container.addSectionHeader(R.string.wg_intention_caption)

        val side = dp(SIDE_DP)
        field = EditText(this).apply {
            setText(saved)
            setSelection(text.length)
            hint = getString(R.string.wg_intention_config_hint)
            filters = arrayOf(InputFilter.LengthFilter(LibraryWidgetPreferences.MAX_INTENTION_LENGTH))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
            imeOptions = EditorInfo.IME_ACTION_DONE
            setHorizontallyScrolling(false)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
            setTextColor(palette.textPrimary)
            setHintTextColor(palette.textSecondary)
            val padding = dp(FIELD_PADDING_DP)
            updatePadding(left = padding, top = padding, right = padding, bottom = padding)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    finish()
                    true
                } else {
                    false
                }
            }
        }
        styleSearchField(field)
        container.addView(
            field,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(side, dp(4), side, 0)
            }
        )

        val description = TextView(this).apply {
            setText(R.string.wg_intention_config_desc)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, DESCRIPTION_SP)
            setTextColor(palette.textSecondary)
        }
        container.addView(
            description,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(side, dp(12), side, 0)
            }
        )
        field.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        val text = LibraryWidgetPreferences.sanitizeIntention(field.text?.toString().orEmpty())
        if (text == saved) return
        if (LibraryWidgetPreferences.setIntention(this, text)) {
            saved = text
            WidgetUpdater.refresh(this, WidgetTrigger.INTENTION)
        } else {
            Toast.makeText(this, R.string.wg_intention_config_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val SIDE_DP = 20
        const val FIELD_PADDING_DP = 16
        const val TEXT_SP = 17f
        const val DESCRIPTION_SP = 13f
    }
}
