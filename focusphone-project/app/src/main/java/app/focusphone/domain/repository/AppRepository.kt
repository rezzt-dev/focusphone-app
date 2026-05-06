package app.focusphone.domain.repository

import app.focusphone.domain.model.AppInfo

/**
 * Provides the list of apps installed on the device.
 * Implementation uses [PackageManager]; no persistence needed.
 */
interface AppRepository {
    /** Returns all user-launchable apps sorted alphabetically by label. */
    suspend fun getInstalledApps(): List<AppInfo>
}
