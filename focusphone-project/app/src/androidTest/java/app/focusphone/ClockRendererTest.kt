package app.focusphone

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.focusphone.domain.model.ClockType
import app.focusphone.ui.widget.ClockRenderer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class ClockRendererTest {

    private lateinit var root: ViewGroup
    private lateinit var renderer: ClockRenderer

    @Before
    fun setUp() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext<Context>(),
            R.style.Theme_FocusPhone_Slate
        )
        root = LayoutInflater.from(context)
            .inflate(R.layout.layout_preview_clock, null, false) as ViewGroup
        renderer = ClockRenderer(context, root, showHiddenPlaceholder = true)
    }

    @Test
    fun one_selected_clock_type_is_visible() {
        renderer.applyType(ClockType.WORDS)

        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.clock_type_words).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.clock_type_digital).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.clock_type_analog).visibility)
    }

    @Test
    fun preview_renders_hidden_clock_placeholder() {
        renderer.applyType(ClockType.HIDDEN)

        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.clock_container).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.clock_type_none).visibility)
    }

    @Test
    fun all_clock_variants_render_the_same_instant() {
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
        }.time

        renderer.render(time)

        assertEquals("09", root.findViewById<TextView>(R.id.tv_stacked_h).text.toString())
        assertEquals("05", root.findViewById<TextView>(R.id.tv_stacked_m).text.toString())
        assertEquals(
            root.context.resources.getStringArray(R.array.clock_words_hours)[9],
            root.findViewById<TextView>(R.id.tv_words_time).text.toString().substringBefore('\n')
        )
    }
}
