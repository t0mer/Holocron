package dev.tomerklein.holocron.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** How a [Rule.senderPattern] is compared against an incoming sender. */
enum class MatchType {
    /** Compare normalized E.164 numbers (default). */
    EXACT,

    /** Raw substring match — also covers alphanumeric sender IDs. */
    CONTAINS,

    /** Regex match against the raw sender string. */
    REGEX,
}

/** A forwarding rule: match an incoming sender, forward to a [Destination]. */
@Entity(
    tableName = "rules",
    indices = [Index("destinationId")],
)
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val senderPattern: String,
    val matchType: MatchType = MatchType.EXACT,
    val destinationId: Long,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
