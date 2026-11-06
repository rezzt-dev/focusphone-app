package app.focusphone.ui.widget.library

import android.content.Context
import android.icu.text.TimeZoneNames
import android.util.Log
import app.focusphone.R
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/** A city the world clock can show. */
data class WorldZone(val id: String) {

    /** The city in the user's language ("londres", "new york"), from ICU's exemplar names. */
    fun city(context: Context): String {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val name = try {
            TimeZoneNames.getInstance(locale).getExemplarLocationName(id)
        } catch (e: RuntimeException) {
            Log.w("WorldZone", "No exemplar name for $id", e)
            null
        }
        return (name ?: WorldClockZones.cityFromId(id)).lowercase(locale)
    }
}

/**
 * The cities offered by the world clock: one or two per populated time zone, so the list
 * stays short enough to scan instead of the four hundred raw zone identifiers.
 */
object WorldClockZones {

    private val available: Set<String> by lazy { TimeZone.getAvailableIDs().toHashSet() }

    val ALL: List<WorldZone> = listOf(
        "Pacific/Honolulu",
        "America/Anchorage",
        "America/Los_Angeles",
        "America/Denver",
        "America/Mexico_City",
        "America/Chicago",
        "America/Bogota",
        "America/New_York",
        "America/Toronto",
        "America/Caracas",
        "America/Santiago",
        "America/Buenos_Aires",
        "America/Sao_Paulo",
        "Atlantic/Reykjavik",
        "Atlantic/Canary",
        "Europe/Lisbon",
        "Europe/London",
        "Europe/Madrid",
        "Europe/Paris",
        "Europe/Berlin",
        "Europe/Rome",
        "Europe/Amsterdam",
        "Europe/Zurich",
        "Europe/Stockholm",
        "Europe/Athens",
        "Africa/Cairo",
        "Africa/Johannesburg",
        "Africa/Lagos",
        "Europe/Istanbul",
        "Europe/Moscow",
        "Asia/Dubai",
        "Asia/Karachi",
        "Asia/Kolkata",
        "Asia/Bangkok",
        "Asia/Singapore",
        "Asia/Hong_Kong",
        "Asia/Shanghai",
        "Asia/Seoul",
        "Asia/Tokyo",
        "Australia/Perth",
        "Australia/Sydney",
        "Pacific/Auckland"
    ).filter { it in available }.map(::WorldZone)

    /** The zone with [id], also when it is not in the curated list (e.g. the device's own). */
    fun find(id: String): WorldZone? = if (id in available) WorldZone(id) else null

    /** "America/New_York" → "New York": the fallback when ICU has no localised name. */
    internal fun cityFromId(id: String): String = id.substringAfterLast('/').replace('_', ' ')

    /**
     * How far [zoneId] is from the device's zone right now: "local", "+8 h", "−5:30 h".
     * Uses the true minus sign so the label reads as a signed number.
     */
    fun offsetLabel(context: Context, zoneId: String, nowMs: Long): String {
        val minutes = offsetMinutes(zoneId, TimeZone.getDefault().id, nowMs)
        return if (minutes == 0) context.getString(R.string.wg_offset_local) else formatOffset(minutes)
    }

    internal fun offsetMinutes(zoneId: String, localId: String, nowMs: Long): Int {
        val zone = TimeZone.getTimeZone(zoneId).getOffset(nowMs)
        val local = TimeZone.getTimeZone(localId).getOffset(nowMs)
        return (zone - local) / MS_PER_MINUTE
    }

    internal fun formatOffset(minutes: Int): String {
        val sign = if (minutes < 0) MINUS else PLUS
        val hours = abs(minutes) / MINUTES_PER_HOUR
        val rest = abs(minutes) % MINUTES_PER_HOUR
        val value = if (rest == 0) "$hours" else "$hours:${rest.toString().padStart(2, '0')}"
        return "$sign$value h"
    }

    private const val MS_PER_MINUTE = 60_000
    private const val MINUTES_PER_HOUR = 60
    private const val PLUS = "+"
    private const val MINUS = "−"
}
