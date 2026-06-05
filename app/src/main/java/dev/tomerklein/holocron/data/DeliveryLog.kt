package dev.tomerklein.holocron.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Lifecycle of a single dispatch attempt. */
enum class DeliveryStatus { PENDING, SUCCESS, FAILED, RETRYING }

/** One forwarding attempt, powering the in-app history screen. */
@Entity(tableName = "delivery_log")
data class DeliveryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long?,
    val destinationId: Long?,
    val sender: String,
    /** Truncated/redactable preview — never the full body in release logging. */
    val bodyPreview: String,
    val timestamp: Long,
    val status: DeliveryStatus,
    val attempts: Int = 0,
    val lastError: String? = null,
)
