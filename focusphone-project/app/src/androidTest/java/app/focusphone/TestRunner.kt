package app.focusphone

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner that uses [TestFocusPhoneApp] as the application class.
 * This allows instrumented tests to inject fake repositories via the app container.
 */
class TestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, TestFocusPhoneApp::class.java.name, context)
    }
}
