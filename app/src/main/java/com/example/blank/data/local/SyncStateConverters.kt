package com.example.blank.data.local

import androidx.room.TypeConverter
import com.example.blank.domain.model.SyncState

class SyncStateConverters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
