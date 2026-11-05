package app.focusphone.ui.widget.library

import android.app.PendingIntent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import app.focusphone.R
import app.focusphone.ui.widget.library.MediaWidgetBridge.State

/**
 * Music widgets: a bar, a full player card and an artwork tile. All three read the same
 * [MediaWidgetBridge.State] and share how each state looks and what a tap does.
 */
object MusicRenderers {

    val Bar = WidgetRenderer { scene ->
        val state = MediaWidgetBridge.state(scene.context)
        scene.views(R.layout.widget_music_bar).apply {
            card(R.id.wg_bg, scene.style)
            bindTrack(scene, state, artSizeDp = 52f)
            bindPlay(scene, state)
            bindSkip(scene, state, R.id.wg_next, WidgetActionReceiver.ACTION_MEDIA_NEXT, state.canNext())
            setOnClickPendingIntent(R.id.wg_root, openTarget(scene, state))
        }
    }

    val Card = WidgetRenderer { scene ->
        val state = MediaWidgetBridge.state(scene.context)
        scene.views(R.layout.widget_music_card).apply {
            card(R.id.wg_bg, scene.style)
            bindTrack(scene, state, artSizeDp = 64f)
            val active = state as? State.Active
            setTextViewText(R.id.wg_app, active?.appLabel.orEmpty())
            show(R.id.wg_app, active != null)
            setTextColor(R.id.wg_app, scene.style.textTertiary)
            bindPlay(scene, state)
            bindSkip(scene, state, R.id.wg_prev, WidgetActionReceiver.ACTION_MEDIA_PREVIOUS, state.canPrevious())
            bindSkip(scene, state, R.id.wg_next, WidgetActionReceiver.ACTION_MEDIA_NEXT, state.canNext())
            setOnClickPendingIntent(R.id.wg_root, openTarget(scene, state))
        }
    }

    /** The artwork is the tile; without one it is a plain tile in the theme's tone. */
    val Square = WidgetRenderer { scene ->
        val style = scene.style
        val context = scene.context
        val state = MediaWidgetBridge.state(context)
        val art = (state as? State.Active)?.artwork
        scene.views(R.layout.widget_music_square).apply {
            card(R.id.wg_bg, style)
            if (art != null) {
                val side = scene.px(scene.size.squareDp.toFloat())
                // The vector body has 24 % corners; the artwork follows it exactly.
                setImageViewBitmap(R.id.wg_art, WidgetBitmaps.artwork(art, side, side * SQUIRCLE_CORNER, scrim = true))
            } else {
                setImageViewBitmap(R.id.wg_art, null)
            }
            show(R.id.wg_art, art != null)
            // The artwork is the tile: a card behind it would show as a rim on its edge.
            show(R.id.wg_bg, art == null)

            val onArt = art != null
            val titleColor = if (onArt) ContextCompat.getColor(context, R.color.wg_on_artwork) else style.textPrimary
            val subtitleColor = if (onArt) ContextCompat.getColor(context, R.color.wg_on_artwork_secondary) else style.textSecondary
            setTextViewText(R.id.wg_title, titleOf(scene, state))
            setTextViewText(R.id.wg_artist, subtitleOf(scene, state))
            setTextColor(R.id.wg_title, titleColor)
            setTextColor(R.id.wg_artist, subtitleColor)
            bindPlay(scene, state)
            if (state == State.NoAccess) {
                // The tile has room for one badge: it asks for the permission.
                show(R.id.wg_play, true)
                tint(R.id.wg_play_fill, style.chip)
                icon(R.id.wg_play_icon, R.drawable.ic_wg_bell, style.textSecondary)
                setOnClickPendingIntent(R.id.wg_play, openTarget(scene, state))
                setContentDescription(R.id.wg_play, scene.string(R.string.wg_music_no_access_title))
            }
            if (onArt) {
                // A white button reads on any artwork, light or dark.
                tint(R.id.wg_play_fill, titleColor)
                tint(R.id.wg_play_icon, ARTWORK_BUTTON_ICON)
            }
            setOnClickPendingIntent(R.id.wg_root, openTarget(scene, state))
        }
    }

    private const val SQUIRCLE_CORNER = 0.14f
    private const val ART_CORNER = 0.2f
    private const val ARTWORK_BUTTON_ICON = 0xFF111111.toInt()

    // ── Shared parts ───────────────────────────────────────────────

    private fun RemoteViews.bindTrack(scene: WidgetScene, state: State, artSizeDp: Float) {
        val style = scene.style
        setTextViewText(R.id.wg_title, titleOf(scene, state))
        setTextViewText(R.id.wg_artist, subtitleOf(scene, state))
        setTextColor(R.id.wg_title, style.textPrimary)
        setTextColor(R.id.wg_artist, style.textSecondary)
        tint(R.id.wg_art_placeholder, style.chip)
        icon(R.id.wg_art_icon, if (state == State.NoAccess) R.drawable.ic_wg_bell else R.drawable.ic_wg_music, style.textSecondary)
        val art = (state as? State.Active)?.artwork
        if (art != null) {
            val side = scene.px(artSizeDp)
            setImageViewBitmap(R.id.wg_art, WidgetBitmaps.artwork(art, side, side * ART_CORNER))
        } else {
            setImageViewBitmap(R.id.wg_art, null)
        }
        show(R.id.wg_art, art != null)
        show(R.id.wg_art_icon, art == null)
    }

    private fun RemoteViews.bindPlay(scene: WidgetScene, state: State) {
        val style = scene.style
        val playing = (state as? State.Active)?.isPlaying == true
        show(R.id.wg_play, state != State.NoAccess)
        tint(R.id.wg_play_fill, style.accent)
        icon(R.id.wg_play_icon, if (playing) R.drawable.ic_wg_pause else R.drawable.ic_wg_play, style.onAccent)
        setContentDescription(R.id.wg_play, scene.string(if (playing) R.string.wg_cd_pause else R.string.wg_cd_play))
        setOnClickPendingIntent(R.id.wg_play, WidgetIntents.action(scene.context, WidgetActionReceiver.ACTION_MEDIA_PLAY_PAUSE))
    }

    private fun RemoteViews.bindSkip(scene: WidgetScene, state: State, id: Int, action: String, enabled: Boolean) {
        show(id, state is State.Active)
        tint(iconOf(id), scene.style.textPrimary)
        setImageViewAlpha(iconOf(id), if (enabled) OPAQUE else DISABLED_ALPHA)
        if (enabled) setOnClickPendingIntent(id, WidgetIntents.action(scene.context, action))
    }

    private fun RemoteViews.setImageViewAlpha(id: Int, alpha: Int) = setInt(id, "setImageAlpha", alpha)

    private fun iconOf(buttonId: Int): Int = when (buttonId) {
        R.id.wg_prev -> R.id.wg_prev_icon
        else -> R.id.wg_next_icon
    }

    private fun titleOf(scene: WidgetScene, state: State): String = when (state) {
        State.NoAccess -> scene.string(R.string.wg_music_no_access_title)
        State.Idle -> scene.string(R.string.wg_music_idle_title)
        is State.Active -> state.title
    }

    private fun subtitleOf(scene: WidgetScene, state: State): String = when (state) {
        State.NoAccess -> scene.string(R.string.wg_music_no_access_subtitle)
        State.Idle -> scene.string(R.string.wg_music_idle_subtitle)
        is State.Active -> state.artist ?: state.appLabel
    }

    /** Tapping the track opens the player that is playing, or the default music app. */
    private fun openTarget(scene: WidgetScene, state: State): PendingIntent = when (state) {
        State.NoAccess -> WidgetIntents.openSystem(scene.context, WidgetIntents.SystemPanel.NOTIFICATION_ACCESS)
        State.Idle -> WidgetIntents.openMusicPlayer(scene.context)
        is State.Active -> WidgetIntents.openApp(scene.context, state.packageName, state.appLabel)
    }

    private fun State.canNext(): Boolean = (this as? State.Active)?.canSkipNext == true

    private fun State.canPrevious(): Boolean = (this as? State.Active)?.canSkipPrevious == true

    private const val OPAQUE = 255
    private const val DISABLED_ALPHA = 90
}
