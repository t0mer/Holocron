package dev.tomerklein.holocron.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Thin façade over the DAOs so the rest of the app never touches Room directly. */
@Singleton
class HolocronRepository @Inject constructor(
    private val ruleDao: RuleDao,
    private val destinationDao: DestinationDao,
    private val deliveryLogDao: DeliveryLogDao,
) {
    fun observeRules(): Flow<List<Rule>> = ruleDao.observeAll()
    fun observeDestinations(): Flow<List<Destination>> = destinationDao.observeAll()
    fun observeRecentLogs(limit: Int = 200): Flow<List<DeliveryLog>> =
        deliveryLogDao.observeRecent(limit)

    suspend fun enabledRules(): List<Rule> = ruleDao.enabledRules()
    suspend fun destination(id: Long): Destination? = destinationDao.byId(id)
    suspend fun rule(id: Long): Rule? = ruleDao.byId(id)

    suspend fun upsertRule(rule: Rule): Long = ruleDao.upsert(rule)
    suspend fun deleteRule(rule: Rule) = ruleDao.delete(rule)
    suspend fun upsertDestination(d: Destination): Long = destinationDao.upsert(d)
    suspend fun deleteDestination(d: Destination) = destinationDao.delete(d)

    suspend fun newPendingLog(log: DeliveryLog): Long = deliveryLogDao.insert(log)
    suspend fun log(id: Long): DeliveryLog? = deliveryLogDao.byId(id)
    suspend fun updateLogStatus(id: Long, status: DeliveryStatus, attempts: Int, error: String?) =
        deliveryLogDao.updateStatus(id, status, attempts, error)

    suspend fun trimLogs(keep: Int) = deliveryLogDao.trimTo(keep)
}
