package app.focusphone.domain.model

/**
 * Result of toggling a favourite app.
 * Used by [app.focusphone.domain.usecase.ToggleFavouriteAppUseCase].
 */
sealed class ToggleResult {
    /** App was added to favourites. */
    object Added : ToggleResult()

    /** App was removed from favourites. */
    object Removed : ToggleResult()

    /**
     * Maximum allowed apps already reached.
     * App was NOT added.
     * @param limit the maximum number of apps allowed.
     */
    data class LimitReached(val limit: Int) : ToggleResult()
}
