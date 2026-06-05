package dev.tomerklein.holocron.dispatch

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tomerklein.holocron.data.DeliveryStatus
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.util.Logx

/**
 * Forwards one matched message to one destination, with WorkManager retry/backoff.
 * Maps [DispatchResult] → [Result] and records the outcome in the delivery log.
 */
@HiltWorker
class DispatchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: HolocronRepository,
    private val registry: DispatcherRegistry,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val logId = inputData.getLong(KEY_LOG_ID, -1L)
        val destinationId = inputData.getLong(KEY_DESTINATION_ID, -1L)
        val ruleId = inputData.getLong(KEY_RULE_ID, -1L)
        val attempt = runAttemptCount + 1

        val destination = repository.destination(destinationId)
        if (destination == null || !destination.enabled) {
            finish(logId, DeliveryStatus.FAILED, attempt, "Destination missing/disabled")
            return Result.failure()
        }

        val ruleName = repository.rule(ruleId)?.name.orEmpty()
        val message = IncomingMessage(
            sender = inputData.getString(KEY_SENDER).orEmpty(),
            body = inputData.getString(KEY_BODY).orEmpty(),
            timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis()),
            defaultRegion = inputData.getString(KEY_REGION),
            ruleName = ruleName,
        )

        if (logId != -1L) {
            repository.updateLogStatus(logId, DeliveryStatus.RETRYING, attempt, null)
        }

        return when (val result = registry.forType(destination.type).send(message, destination)) {
            is DispatchResult.Success -> {
                finish(logId, DeliveryStatus.SUCCESS, attempt, null)
                Result.success()
            }
            is DispatchResult.Retryable -> {
                Logx.w(TAG, "Retryable dispatch failure (attempt $attempt): ${result.reason}")
                if (attempt >= MAX_ATTEMPTS) {
                    finish(logId, DeliveryStatus.FAILED, attempt, "Gave up: ${result.reason}")
                    Result.failure()
                } else {
                    finish(logId, DeliveryStatus.RETRYING, attempt, result.reason)
                    Result.retry()
                }
            }
            is DispatchResult.Permanent -> {
                finish(logId, DeliveryStatus.FAILED, attempt, result.reason)
                Result.failure()
            }
        }
    }

    private suspend fun finish(logId: Long, status: DeliveryStatus, attempt: Int, error: String?) {
        if (logId != -1L) repository.updateLogStatus(logId, status, attempt, error)
    }

    companion object {
        const val KEY_LOG_ID = "logId"
        const val KEY_RULE_ID = "ruleId"
        const val KEY_DESTINATION_ID = "destinationId"
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_REGION = "region"

        const val MAX_ATTEMPTS = 5
        private const val TAG = "DispatchWorker"
    }
}
