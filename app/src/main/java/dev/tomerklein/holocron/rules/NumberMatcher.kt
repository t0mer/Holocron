package dev.tomerklein.holocron.rules

import com.google.i18n.phonenumbers.PhoneNumberUtil
import dev.tomerklein.holocron.data.MatchType
import dev.tomerklein.holocron.data.Rule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether an incoming sender matches a [Rule].
 *
 * EXACT compares E.164-normalized numbers (so `+972…`, `0…`, and spaced/formatted
 * variants match). CONTAINS/REGEX operate on the raw sender string, which is the path
 * for alphanumeric sender IDs (e.g. "MyBank") that don't normalize.
 *
 * [defaultRegion] is the device's default region (ISO-3166 alpha-2, e.g. "IL"), used as a
 * fallback when parsing local-format numbers.
 */
@Singleton
class NumberMatcher @Inject constructor(
    private val phoneUtil: PhoneNumberUtil,
) {
    fun matches(rule: Rule, rawSender: String, defaultRegion: String?): Boolean = when (rule.matchType) {
        MatchType.EXACT -> {
            val a = normalize(rawSender, defaultRegion)
            val b = normalize(rule.senderPattern, defaultRegion)
            a != null && a == b
        }
        MatchType.CONTAINS -> rawSender.contains(rule.senderPattern, ignoreCase = true)
        MatchType.REGEX -> runCatching { Regex(rule.senderPattern).containsMatchIn(rawSender) }
            .getOrDefault(false)
    }

    /**
     * Returns the E.164 form, or null if the value isn't a phone number.
     *
     * Gated on [PhoneNumberUtil.isPossibleNumber], not `isValidNumber`: matching only needs
     * consistent normalization across formats, and `isValidNumber` would reject numbers whose
     * subscriber range isn't a real assigned range. Alphanumeric sender IDs fail to parse and
     * return null here (they belong on CONTAINS/REGEX rules).
     */
    fun normalize(value: String, defaultRegion: String?): String? = runCatching {
        val region = defaultRegion?.takeIf { it.isNotBlank() }?.uppercase()
        val parsed = phoneUtil.parse(value, region)
        if (!phoneUtil.isPossibleNumber(parsed)) return null
        phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
    }.getOrNull()
}
