package app.focusphone.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import app.focusphone.databinding.ActivitySettingsSubBinding
import app.focusphone.ui.base.BaseActivity

/**
 * Abstract base class for second-level settings screens.
 *
 * Provides a consistent toolbar, back button, edge-to-edge insets and a
 * [LinearLayout] content container that subclasses populate.
 */
abstract class SettingsSubActivity : BaseActivity() {

    protected lateinit var binding: ActivitySettingsSubBinding
    protected val contentContainer: LinearLayout get() = binding.contentContainer
    protected val inflater: LayoutInflater get() = LayoutInflater.from(this)

    @StringRes
    protected abstract fun getScreenTitleRes(): Int

    /** Called once the content container is ready. Subclasses should inflate rows here. */
    protected abstract fun buildContent(container: LinearLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsSubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        binding.tvTitle.setText(getScreenTitleRes())
        binding.btnBack.setOnClickListener { finish() }
        buildContent(binding.contentContainer)
        applyFontFamilyToView(binding.root)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.scrollContent.updatePadding(bottom = bars.bottom)
            insets
        }
    }
}
