package app.focusphone.ui.tutorial

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.focusphone.R

/** A presentation-neutral page used by onboarding, the guide and contextual coach marks. */
data class TutorialPage(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    @param:StringRes val actionRes: Int? = null,
    @param:StringRes val taglineRes: Int? = null,
    val action: TutorialAction = TutorialAction.NONE,
    val topic: TutorialTopic? = null
)

enum class TutorialAction { NONE, OPEN_LAUNCHER_SETTINGS, OPEN_APP_SELECTOR, COMPLETE }

enum class TutorialTopic { HOME, APPS, MENU, PROFILE, BLOCK, RENAME, DETOX, CUSTOMIZE }

/** Single source of truth for every tutorial presenter in the app. */
object TutorialContent {

    val onboardingPages: List<TutorialPage> = listOf(
        TutorialPage(
            iconRes = R.drawable.ic_tutorial_home,
            titleRes = R.string.onboarding_step_welcome_title,
            bodyRes = R.string.onboarding_step_welcome_body,
            taglineRes = R.string.onboarding_step_welcome_tagline
        ),
        TutorialPage(
            iconRes = R.drawable.ic_tutorial_pause,
            titleRes = R.string.onboarding_step_philosophy_title,
            bodyRes = R.string.onboarding_step_philosophy_body
        ),
        TutorialPage(
            iconRes = R.drawable.ic_tutorial_home,
            titleRes = R.string.onboarding_step_launcher_title,
            bodyRes = R.string.onboarding_step_launcher_body,
            actionRes = R.string.onboarding_step_launcher_action,
            action = TutorialAction.OPEN_LAUNCHER_SETTINGS
        ),
        TutorialPage(
            iconRes = R.drawable.ic_tutorial_edit,
            titleRes = R.string.onboarding_step_apps_title,
            bodyRes = R.string.onboarding_step_apps_body,
            actionRes = R.string.onboarding_step_apps_action,
            action = TutorialAction.OPEN_APP_SELECTOR
        ),
        TutorialPage(
            iconRes = R.drawable.ic_tutorial_home,
            titleRes = R.string.onboarding_step_complete_title,
            bodyRes = R.string.onboarding_step_complete_body,
            actionRes = R.string.onboarding_step_complete_action,
            action = TutorialAction.COMPLETE
        )
    )

    /** Full guide; the home overlay selects the topics that have a live target. */
    val guidePages: List<TutorialPage> = listOf(
        guide(TutorialTopic.HOME, R.drawable.ic_tutorial_home, R.string.tutorial_coach_home_title, R.string.tutorial_coach_home_desc),
        guide(TutorialTopic.APPS, R.drawable.ic_add, R.string.tutorial_coach_apps_title, R.string.tutorial_coach_apps_desc),
        guide(TutorialTopic.MENU, R.drawable.ic_tutorial_home, R.string.tutorial_coach_menu_title, R.string.tutorial_coach_menu_desc),
        guide(
            TutorialTopic.PROFILE,
            R.drawable.ic_tutorial_profiles,
            R.string.tutorial_coach_profile_title,
            R.string.tutorial_coach_profile_desc
        ),
        guide(TutorialTopic.BLOCK, R.drawable.ic_tutorial_block, R.string.tutorial_coach_block_title, R.string.tutorial_coach_block_desc),
        guide(TutorialTopic.RENAME, R.drawable.ic_tutorial_edit, R.string.tutorial_coach_rename_title, R.string.tutorial_coach_rename_desc),
        guide(TutorialTopic.DETOX, R.drawable.ic_tutorial_detox, R.string.tutorial_coach_detox_title, R.string.tutorial_coach_detox_desc),
        guide(
            TutorialTopic.CUSTOMIZE,
            R.drawable.ic_tutorial_clock,
            R.string.tutorial_coach_customize_title,
            R.string.tutorial_coach_customize_desc
        )
    )

    fun guidePage(topic: TutorialTopic): TutorialPage =
        requireNotNull(guidePages.firstOrNull { it.topic == topic }) { "No tutorial page for $topic" }

    private fun guide(
        topic: TutorialTopic,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes body: Int
    ) = TutorialPage(icon, title, body, topic = topic)
}
