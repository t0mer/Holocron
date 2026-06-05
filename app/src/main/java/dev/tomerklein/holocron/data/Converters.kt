package dev.tomerklein.holocron.data

import androidx.room.TypeConverter

/** Room enum <-> String converters. */
class Converters {
    @TypeConverter fun fromMatchType(v: MatchType): String = v.name
    @TypeConverter fun toMatchType(v: String): MatchType = MatchType.valueOf(v)

    @TypeConverter fun fromDestinationType(v: DestinationType): String = v.name
    @TypeConverter fun toDestinationType(v: String): DestinationType = DestinationType.valueOf(v)

    @TypeConverter fun fromDeliveryStatus(v: DeliveryStatus): String = v.name
    @TypeConverter fun toDeliveryStatus(v: String): DeliveryStatus = DeliveryStatus.valueOf(v)
}
