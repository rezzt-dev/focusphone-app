package app.focusphone.service

import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FocusAccessibilityServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getPackageName(): String = "app.focusphone"
        }
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
    }

    @Test
    fun `disabled service returns false`() {
        assertFalse(FocusAccessibilityService.isEnabled(context))
    }

    @Test
    fun `full component form is recognized`() {
        setEnabled("app.focusphone/app.focusphone.service.FocusAccessibilityService")
        assertTrue(FocusAccessibilityService.isEnabled(context))
    }

    @Test
    fun `short component form used by OEMs is recognized`() {
        setEnabled("app.focusphone/.service.FocusAccessibilityService")
        assertTrue(FocusAccessibilityService.isEnabled(context))
    }

    @Test
    fun `active app wins over recents event source`() {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOWS_CHANGED).apply {
            packageName = "com.android.systemui"
        }

        assertEquals("com.example.blocked", foregroundPackageForEvent(event, listOf(window("com.example.blocked"))))
    }

    @Test
    fun `window change never treats recents event source as foreground app`() {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOWS_CHANGED).apply {
            packageName = "com.android.systemui"
        }

        assertNull(foregroundPackageForEvent(event, emptyList()))
    }

    @Test
    fun `state change uses event package when windows are temporarily unavailable`() {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = "com.example.blocked"
        }

        assertEquals("com.example.blocked", foregroundPackageForEvent(event, emptyList()))
    }

    private fun window(packageName: String): AccessibilityWindowInfo {
        val root = mockk<AccessibilityNodeInfo>()
        every { root.packageName } returns packageName
        val window = mockk<AccessibilityWindowInfo>()
        every { window.isActive } returns true
        every { window.isFocused } returns true
        every { window.root } returns root
        return window
    }

    private fun setEnabled(value: String) {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            value
        )
    }
}
