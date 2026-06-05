package dev.tomerklein.holocron.rules

import com.google.i18n.phonenumbers.PhoneNumberUtil
import dev.tomerklein.holocron.data.MatchType
import dev.tomerklein.holocron.data.Rule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberMatcherTest {

    private val matcher = NumberMatcher(PhoneNumberUtil.getInstance())

    private fun rule(pattern: String, type: MatchType) = Rule(
        name = "test",
        senderPattern = pattern,
        matchType = type,
        destinationId = 1,
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun exact_matchesAcrossLocalAndE164Forms() {
        val r = rule("050-123-4567", MatchType.EXACT)
        assertTrue(matcher.matches(r, "+972501234567", "IL"))
        assertTrue(matcher.matches(r, "0501234567", "IL"))
    }

    @Test
    fun exact_rejectsDifferentNumber() {
        val r = rule("0501234567", MatchType.EXACT)
        assertFalse(matcher.matches(r, "+972529999999", "IL"))
    }

    @Test
    fun contains_matchesAlphanumericSenderId() {
        val r = rule("Bank", MatchType.CONTAINS)
        assertTrue(matcher.matches(r, "MyBank", null))
        assertFalse(matcher.matches(r, "Telco", null))
    }

    @Test
    fun regex_matchesPattern() {
        val r = rule("^My.*$", MatchType.REGEX)
        assertTrue(matcher.matches(r, "MyBank", null))
        assertFalse(matcher.matches(r, "TheirBank", null))
    }

    @Test
    fun regex_invalidPatternDoesNotThrow() {
        val r = rule("([unclosed", MatchType.REGEX)
        assertFalse(matcher.matches(r, "anything", null))
    }
}
