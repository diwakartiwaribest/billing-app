package com.shop.billing.data.local.converter

import androidx.room.TypeConverter
import com.shop.billing.data.local.entity.SyncStatus
import java.time.Instant

class Converters {
    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
    @TypeConverter fun fromSyncStatus(value: SyncStatus): String = value.name
    @TypeConverter fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
