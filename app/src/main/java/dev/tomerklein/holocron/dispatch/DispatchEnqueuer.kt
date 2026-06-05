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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Records a PENDING delivery and enqueues a retrying WorkManager job to forward it. */
@Singleton
class DispatchEnqueuer @Inject constructor(
    private val repository: HolocronRepository,
    private val workManager: WorkManager,
) {
    suspend fun enqueue(message: IncomingMessage, rule: Rule) {
        val logId = repository.newPendingLog(
            DeliveryLog(
                ruleId = rule.id,
                destinationId = rule.destinationId,
                sender = message.sender,
                bodyPreview = message.body.take(BODY_PREVIEW_LEN),
                timestamp = message.timestamp,
                status = DeliveryStatus.PENDING,
            ),
        )

        val input = Data.Builder()
            .putLong(DispatchWorker.KEY_LOG_ID, logId)
            .putLong(DispatchWorker.KEY_RULE_ID, rule.id)
            .putLong(DispatchWorker.KEY_DESTINATION_ID, rule.destinationId)
            .putString(DispatchWorker.KEY_SENDER, message.sender)
            .putString(DispatchWorker.KEY_BODY, message.body)
            .putLong(DispatchWorker.KEY_TIMESTAMP, message.timestamp)
            .putString(DispatchWorker.KEY_REGION, message.defaultRegion)
            .build()

        val request = OneTimeWorkRequestBuilder<DispatchWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInputData(input)
            .addTag(TAG_DISPATCH)
            .build()

        workManager.enqueue(request)
    }

    private companion object {
        const val BODY_PREVIEW_LEN = 120
        const val TAG_DISPATCH = "holocron-dispatch"
    }
}
