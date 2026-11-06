package app.focusphone.ui.widget.library

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import app.focusphone.service.FocusNotificationService

/**
 * Connects the music widgets to whatever is playing.
 *
 * Android only lets the app holding notification access read other apps' media sessions,
 * so this goes through [FocusNotificationService]: the widgets ask for the user's
 * permission when it is missing, and while the listener runs it reports every change of
 * track or playback state here, which redraws the music widgets.
 */
object MediaWidgetBridge {

    private const val TAG = "MediaWidgetBridge"
    private const val REFRESH_DELAY_MS = 250L
    private const val AFTER_ACTION_DELAY_MS = 700L

    /** What the music widgets show. */
    sealed interface State {
        /** Notification access was not granted, so no session can be read. */
        data object NoAccess : State

        /** Nothing is playing or paused. */
        data object Idle : State

        data class Active(
            val packageName: String,
            val appLabel: String,
            val title: String,
            val artist: String?,
            val artwork: Bitmap?,
            val isPlaying: Boolean,
            val canSkipPrevious: Boolean,
            val canSkipNext: Boolean
        ) : State
    }

    // ── Reading ────────────────────────────────────────────────────

    fun state(context: Context): State {
        if (!FocusNotificationService.isEnabled(context)) return State.NoAccess
        val controller = activeController(context) ?: return State.Idle
        val metadata = controller.metadata
        val description = metadata?.description
        val title = description?.title?.toString()?.trim().orEmpty()
        if (title.isEmpty() && controller.playbackState?.state != PlaybackState.STATE_PLAYING) return State.Idle
        val playback = controller.playbackState
        val actions = playback?.actions ?: 0L
        val appLabel = labelOf(context, controller.packageName)
        return State.Active(
            packageName = controller.packageName,
            appLabel = appLabel,
            title = title.ifEmpty { appLabel },
            artist = (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: description?.subtitle?.toString())
                ?.trim()?.takeIf { it.isNotEmpty() },
            artwork = metadata?.let(::artworkOf),
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING || playback?.state == PlaybackState.STATE_BUFFERING,
            canSkipPrevious = playback == null || actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            canSkipNext = playback == null || actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        )
    }

    private fun artworkOf(metadata: MediaMetadata): Bitmap? =
        metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    /** The session to show: the one playing, else the most recent one. */
    private fun activeController(context: Context): MediaController? = sessions(context).let { list ->
        list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
    }

    private fun sessions(context: Context): List<MediaController> = try {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        manager?.getActiveSessions(listenerComponent(context)).orEmpty()
    } catch (e: SecurityException) {
        // Access was revoked between the check and the call.
        Log.w(TAG, "Media sessions are not readable", e)
        emptyList()
    }

    private fun listenerComponent(context: Context) = ComponentName(context, FocusNotificationService::class.java)

    private fun labelOf(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString().lowercase()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    // ── Controls ───────────────────────────────────────────────────

    /**
     * Toggles playback. With no session at all, a play key goes to the system, which hands
     * it to the last app that played — the usual "resume my music" behaviour of headsets.
     */
    fun playPause(context: Context) {
        val controller = activeController(context)
        if (controller == null) {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        } else if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
        refreshAfterAction(context)
    }

    fun skipNext(context: Context) {
        activeController(context)?.transportControls?.skipToNext()
        refreshAfterAction(context)
    }

    fun skipPrevious(context: Context) {
        activeController(context)?.transportControls?.skipToPrevious()
        refreshAfterAction(context)
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val time = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0))
    }

    // ── Live updates ───────────────────────────────────────────────

    private val main = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private val watched = mutableMapOf<MediaSession.Token, MediaController>()
    private var lastSignature: String? = null

    private val refreshTask = Runnable { refreshIfChanged() }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        watch(controllers.orEmpty())
        scheduleRefresh(null)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = scheduleRefresh(null)

        override fun onMetadataChanged(metadata: MediaMetadata?) = scheduleRefresh(null)

        override fun onSessionDestroyed() = scheduleRefresh(null)
    }

    /** Starts following media sessions. Called by the listener once it is connected. */
    fun attach(context: Context) {
        main.post {
            val app = context.applicationContext
            appContext = app
            try {
                val manager = app.getSystemService(MediaSessionManager::class.java) ?: return@post
                manager.removeOnActiveSessionsChangedListener(sessionsListener)
                manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent(app), main)
                watch(manager.getActiveSessions(listenerComponent(app)))
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not follow media sessions", e)
            }
            lastSignature = null
            scheduleRefresh(null)
        }
    }

    /** Stops following media sessions; the widgets fall back to their idle state. */
    fun detach(context: Context) {
        main.post {
            val app = context.applicationContext
            try {
                app.getSystemService(MediaSessionManager::class.java)?.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Could not stop following media sessions", e)
            }
            watch(emptyList())
            lastSignature = null
            WidgetUpdater.refresh(app, WidgetTrigger.MEDIA)
        }
    }

    private fun watch(controllers: List<MediaController>) {
        val next = controllers.associateBy { it.sessionToken }
        (watched.keys - next.keys).forEach { watched.remove(it)?.unregisterCallback(controllerCallback) }
        (next.keys - watched.keys).forEach { key ->
            val controller = next.getValue(key)
            controller.registerCallback(controllerCallback, main)
            watched[key] = controller
        }
    }

    /**
     * A player takes a moment to report the new state after a button press, and the
     * listener may not be running to report it at all; one forced redraw covers both.
     */
    private fun refreshAfterAction(context: Context) {
        val app = context.applicationContext
        main.postDelayed({
            lastSignature = null
            WidgetUpdater.refresh(app, WidgetTrigger.MEDIA)
        }, AFTER_ACTION_DELAY_MS)
    }

    private fun scheduleRefresh(context: Context?) {
        if (context != null) appContext = context.applicationContext
        main.removeCallbacks(refreshTask)
        main.postDelayed(refreshTask, REFRESH_DELAY_MS)
    }

    /**
     * Playback states also change for position updates; the widgets show no position, so
     * they are only redrawn when something they display actually changed.
     */
    private fun refreshIfChanged() {
        val context = appContext ?: return
        val signature = when (val state = state(context)) {
            State.NoAccess -> "no-access"

            State.Idle -> "idle"

            is State.Active -> listOf(
                state.packageName,
                state.title,
                state.artist,
                state.isPlaying,
                state.artwork?.generationId,
                state.canSkipNext,
                state.canSkipPrevious
            ).joinToString("|")
        }
        if (signature == lastSignature) return
        lastSignature = signature
        WidgetUpdater.refresh(context, WidgetTrigger.MEDIA)
    }
}
