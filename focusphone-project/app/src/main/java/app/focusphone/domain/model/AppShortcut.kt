package app.focusphone.domain.model

/**
 * Immutable domain representation of an app shortcut.
 * Shortcuts are retrieved via LauncherApps and can be dynamic, manifest-declared, or pinned.
 */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val shortLabel: String,
    val longLabel: String?
)
