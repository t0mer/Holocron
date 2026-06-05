package dev.tomerklein.holocron.ingest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnqueueDedupTest {

    @Test
    fun allowsFirstAndSuppressesDuplicateWithinWindow() {
        val dedup = EnqueueDedup(windowMs = 8_000L)
        assertTrue(dedup.allow("1|hello", 1_000))
        // Same key shortly after (e.g. SMS broadcast + messaging-app notification) is suppressed.
        assertFalse(dedup.allow("1|hello", 3_000))
    }

    @Test
    fun allowsSameBodyForDifferentRules() {
        val dedup = EnqueueDedup(windowMs = 8_000L)
        assertTrue(dedup.allow("1|hello", 1_000))
        assertTrue(dedup.allow("2|hello", 1_000))
    }

    @Test
    fun allowsAgainAfterWindowElapses() {
        val dedup = EnqueueDedup(windowMs = 8_000L)
        assertTrue(dedup.allow("1|hello", 1_000))
        assertFalse(dedup.allow("1|hello", 5_000))
        assertTrue(dedup.allow("1|hello", 10_000))
    }

    @Test
    fun differentBodiesAreIndependent() {
        val dedup = EnqueueDedup(windowMs = 8_000L)
        assertTrue(dedup.allow("1|hello", 1_000))
        assertTrue(dedup.allow("1|world", 1_500))
    }
}
