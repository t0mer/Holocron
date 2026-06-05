package dev.tomerklein.holocron.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Rule::class, Destination::class, DeliveryLog::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HolocronDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun destinationDao(): DestinationDao
    abstract fun deliveryLogDao(): DeliveryLogDao
}
