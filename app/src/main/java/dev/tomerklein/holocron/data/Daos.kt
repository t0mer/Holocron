package dev.tomerklein.holocron.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY name")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun enabledRules(): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun byId(id: Long): Rule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: Rule): Long

    @Update
    suspend fun update(rule: Rule)

    @Delete
    suspend fun delete(rule: Rule)
}

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations ORDER BY name")
    fun observeAll(): Flow<List<Destination>>

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun byId(id: Long): Destination?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(destination: Destination): Long

    @Update
    suspend fun update(destination: Destination)

    @Delete
    suspend fun delete(destination: Destination)
}

@Dao
interface DeliveryLogDao {
    @Query("SELECT * FROM delivery_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<DeliveryLog>>

    @Query("SELECT * FROM delivery_log WHERE id = :id")
    suspend fun byId(id: Long): DeliveryLog?

    @Insert
    suspend fun insert(log: DeliveryLog): Long

    @Update
    suspend fun update(log: DeliveryLog)

    @Query(
        "UPDATE delivery_log SET status = :status, attempts = :attempts, lastError = :error " +
            "WHERE id = :id",
    )
    suspend fun updateStatus(id: Long, status: DeliveryStatus, attempts: Int, error: String?)

    /** Retention: keep only the newest [keep] rows. */
    @Query(
        "DELETE FROM delivery_log WHERE id NOT IN " +
            "(SELECT id FROM delivery_log ORDER BY timestamp DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)
}
