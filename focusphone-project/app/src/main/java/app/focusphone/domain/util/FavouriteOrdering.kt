package app.focusphone.domain.util

/**
 * Pure ordering rule for the favourite apps.
 *
 * Reordering must never be able to lose a favourite, so it is expressed as a merge rather
 * than as "replace the table with this list": the requested order comes first, and every
 * stored favourite the request did not mention keeps its previous relative position after
 * them.
 */
object FavouriteOrdering {

    /**
     * Returns every package of [current] exactly once.
     *
     * @param current the stored favourites, in their stored order.
     * @param requested the order the user asked for; unknown or repeated packages are ignored.
     */
    fun merge(current: List<String>, requested: List<String>): List<String> {
        val known = current.toSet()
        val head = requested.filter { it in known }.distinct()
        val headSet = head.toSet()
        return head + current.filterNot { it in headSet }
    }
}
