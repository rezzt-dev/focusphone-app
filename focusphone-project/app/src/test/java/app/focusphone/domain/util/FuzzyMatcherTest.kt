package app.focusphone.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    @Test
    fun `empty query matches everything`() {
        assertTrue(FuzzyMatcher.matches("", "facebook"))
        assertTrue(FuzzyMatcher.matches("   ", "youtube"))
    }

    @Test
    fun `exact substring matches`() {
        assertTrue(FuzzyMatcher.matches("face", "facebook"))
        assertTrue(FuzzyMatcher.matches("tube", "youtube"))
    }

    @Test
    fun `fuzzy subsequence matches with gaps`() {
        assertTrue(FuzzyMatcher.matches("fb", "facebook"))
        assertTrue(FuzzyMatcher.matches("yt", "youtube"))
        assertTrue(FuzzyMatcher.matches("msg", "messages"))
        assertTrue(FuzzyMatcher.matches("maps", "google maps"))
    }

    @Test
    fun `case insensitive matching`() {
        assertTrue(FuzzyMatcher.matches("FB", "Facebook"))
        assertTrue(FuzzyMatcher.matches("Yt", "YouTube"))
    }

    @Test
    fun `non matching query returns false`() {
        assertFalse(FuzzyMatcher.matches("zzz", "facebook"))
        assertFalse(FuzzyMatcher.matches("xyz", "youtube"))
    }

    @Test
    fun `out of order characters do not match`() {
        assertFalse(FuzzyMatcher.matches("bf", "facebook"))
    }

    @Test
    fun `consecutive matches score higher than gapped matches`() {
        val consecutive = FuzzyMatcher.score("face", "facebook")
        val gapped = FuzzyMatcher.score("fabc", "facebook")
        assertTrue("consecutive score should be higher", consecutive > gapped)
    }

    @Test
    fun `score is zero when characters are missing`() {
        assertEquals(0, FuzzyMatcher.score("zzz", "facebook"))
    }

    @Test
    fun `minScore threshold filters weak matches`() {
        assertFalse(FuzzyMatcher.matches("f", "facebook", minScore = 5))
        assertTrue(FuzzyMatcher.matches("face", "facebook", minScore = 5))
    }

    @Test
    fun `query longer than target does not match`() {
        assertFalse(FuzzyMatcher.matches("facebook", "face"))
        assertEquals(0, FuzzyMatcher.score("aaaa", "aaa"))
    }

    @Test
    fun `empty target does not match non-empty query`() {
        assertFalse(FuzzyMatcher.matches("a", ""))
    }

    @Test
    fun `search is accent-insensitive`() {
        // A query typed without accents must still find an accented app name (FP-056):
        // display keeps "Teléfono", but searching for "telefono" has to match it.
        assertTrue(FuzzyMatcher.matches("cafe", "café"))
        assertTrue(FuzzyMatcher.matches("caf", "café"))
    }

    @Test
    fun `query with spaces matches if spaces exist in target`() {
        assertTrue(FuzzyMatcher.matches("google maps", "google maps"))
        // Subsequence matcher skips non-matching characters (including spaces),
        // so "googlemaps" IS a subsequence of "google maps".
        assertTrue(FuzzyMatcher.matches("googlemaps", "google maps"))
    }

    @Test
    fun `single character query matches`() {
        assertTrue(FuzzyMatcher.matches("f", "facebook"))
        assertTrue(FuzzyMatcher.matches("z", "zillow"))
    }

    @Test
    fun `repeated characters match correctly`() {
        assertTrue(FuzzyMatcher.matches("oo", "facebook"))
        assertTrue(FuzzyMatcher.matches("book", "facebook"))
    }
}
