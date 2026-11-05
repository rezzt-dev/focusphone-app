package app.focusphone.ui.widget

import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater

/**
 * Context handed to every [android.appwidget.AppWidgetHostView] FocusPhone creates.
 *
 * `RemoteViews` inflates a provider's layout with `LayoutInflater.from(hostContext)` cloned
 * into the provider's package. An Activity's inflater carries AppCompat's factory, which
 * swaps each platform `TextView` for an `AppCompatTextView` / `MaterialTextView` that reads
 * FocusPhone's own attribute IDs against the *provider's* resources. Those IDs mean
 * something else there, so inflation failed (`Can't convert value at index N to dimension`)
 * and every widget showed the platform's "couldn't add widget" error instead of its content.
 *
 * This wrapper keeps the Activity as base — clicks inside the widget still start their
 * activities from a window — but answers with the application's inflater, which has no
 * factory installed.
 */
class WidgetHostContext(base: Context) : ContextWrapper(base) {

    private val cleanInflater: LayoutInflater by lazy {
        LayoutInflater.from(base.applicationContext).cloneInContext(this)
    }

    override fun getSystemService(name: String): Any? =
        if (name == LAYOUT_INFLATER_SERVICE) cleanInflater else super.getSystemService(name)
}
