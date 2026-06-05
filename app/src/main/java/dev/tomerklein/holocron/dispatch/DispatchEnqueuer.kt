package dev.tomerklein.holocron.dispatch

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.tomerklein.holocron.data.DeliveryLog
import dev.tomerklein.holocron.data.DeliveryStatus
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.Rule
import dev.tomerklein.holocron.data.SecurePrefs
import dev.tomerklein.holocron.data.SettingsStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Records a PENDING delivery and enqueues a retrying WorkManager job to forward it. */
@Singleton
class DispatchEnqueuer @Inject constructor(
    private val repository: HolocronRepository,
    private val workManager: WorkManager,
    private val settingsStore: SettingsStore,
    private val securePrefs: SecurePrefs,
) {
    /** Enqueue a forward for a freshly received message matched to [rule]. */
    suspend fun enqueue(message: IncomingMessage, rule: Rule) {
        val settings = settingsStore.settings.first()
        val preview = if (settings.redactBodies) {
            "<redacted ${message.body.length} chars>"
        } else {
            message.body.take(BODY_PREVIEW_LEN)
        }
        val logId = repository.newPendingLog(
            DeliveryLog(
                ruleId = rule.id,
                destinationId = rule.destinationId,
                sender = message.sender,
                bodyPreview = preview,
                timestamp = message.timestamp,
                status = DeliveryStatus.PENDING,
            ),
        )
        repository.trimLogs(settings.logRetention)

        securePrefs.putPendingBody(logId, message.body)
        workManager.enqueue(
            buildRequest(
                logId = logId,
                ruleId = rule.id,
                destinationId = rule.destinationId,
                sender = message.sender,
                timestamp = message.timestamp,
                region = message.defaultRegion,
            ),
        )
    }

    /** Re-run a previously FAILED delivery, if its encrypted body is still available. */
    suspend fun retry(log: DeliveryLog): Boolean {
        if (securePrefs.getPendingBody(log.id) == null) return false
        repository.updateLogStatus(log.id, DeliveryStatus.PENDING, log.attempts, null)
        workManager.enqueue(
            buildRequest(
                logId = log.id,
                ruleId = log.ruleId ?: -1L,
                destinationId = log.destinationId ?: -1L,
                sender = log.sender,
                timestamp = log.timestamp,
                region = null,
            ),
        )
        return true
    }

    private fun buildRequest(
        logId: Long,
        ruleId: Long,
        destinationId: Long,
        sender: String,
        timestamp: Long,
        region: String?,
    ) = OneTimeWorkRequestBuilder<DispatchWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .setInputData(
            Data.Builder()
                .putLong(DispatchWorker.KEY_LOG_ID, logId)
                .putLong(DispatchWorker.KEY_RULE_ID, ruleId)
                .putLong(DispatchWorker.KEY_DESTINATION_ID, destinationId)
                .putString(DispatchWorker.KEY_SENDER, sender)
                .putLong(DispatchWorker.KEY_TIMESTAMP, timestamp)
                .putString(DispatchWorker.KEY_REGION, region)
                .build(),
        )
        .addTag(TAG_DISPATCH)
        .build()

    private companion object {
        const val BODY_PREVIEW_LEN = 120
        const val TAG_DISPATCH = "holocron-dispatch"
    }
}
