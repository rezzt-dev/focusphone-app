package app.focusphone.domain.util

/**
 * Lightweight fuzzy subsequence matcher for app name search.
 *
 * Scoring rules:
 *  - +1 for every matched character.
 *  - +3 consecutive bonus when the next query character immediately follows
 *    the previous matched character in the target string.
 *
 * A match requires every query character to appear in the target in order.
 * The minimum score threshold filters out weak matches.
 *
 * Examples:
 *  - "fb" matches "facebook" (high score thanks to consecutive bonus).
 *  - "yt" matches "youtube".
 *  - "zzz" does not match "facebook".
 */
object FuzzyMatcher {

    private const val CONSECUTIVE_BONUS = 3

    /**
     * Returns a positive score if [query] is a subsequence of [target],
     * or zero if any query character is missing.
     *
     * Performance: O(|query| × |target|) in the worst case, but typically
     * closer to O(|target|) because the inner loop advances monotonically.
     */
    fun score(query: String, target: String): Int {
        val q = query.lowercase()
        val t = target.lowercase()

        // Early exit: not enough characters in target to form the subsequence.
        if (q.length > t.length) return 0

        var score = 0
        var tIndex = 0
        var consecutiveBonus = 0

        for (qc in q) {
            var found = false
            while (tIndex < t.length) {
                if (t[tIndex] == qc) {
                    score += 1 + consecutiveBonus
                    consecutiveBonus = CONSECUTIVE_BONUS
                    found = true
                    tIndex++
                    break
                }
                consecutiveBonus = 0
                tIndex++
            }
            if (!found) return 0
        }
        return score
    }

    /**
     * Returns true when [query] is a subsequence of [target] and the score
     * is at least [minScore]. An empty or blank query always matches.
     */
    fun matches(query: String, target: String, minScore: Int = 1): Boolean {
        if (query.isBlank()) return true
        return score(query, target) >= minScore
    }
}
