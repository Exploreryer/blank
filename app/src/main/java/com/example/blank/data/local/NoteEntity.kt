package com.example.blank.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.blank.domain.model.SyncState
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
    val syncState: SyncState = SyncState.PENDING,
    val driveFileId: String? = null,
    val contentHash: String = "",
    val lastSyncedAt: Long? = null,
    val deletedAt: Long? = null,
    val conflictOf: String? = null
)
