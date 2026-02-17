package com.example.quickdraft.sync

import com.example.quickdraft.data.local.NoteEntity
import com.example.quickdraft.data.remote.drive.RemoteNote
import com.example.quickdraft.domain.model.SyncState
import java.util.UUID

class ConflictResolver {
    fun createConflictCopy(remote: RemoteNote, local: NoteEntity): NoteEntity {
        return NoteEntity(
            id = UUID.randomUUID().toString(),
            content = remote.content,
            createdAt = System.currentTimeMillis(),
            updatedAt = remote.updatedAt,
            isStarred = false,
            isArchived = local.isArchived,
            syncState = SyncState.CONFLICT,
            driveFileId = remote.fileId,
            contentHash = remote.contentHash,
            lastSyncedAt = remote.updatedAt,
            conflictOf = local.id
        )
    }
}
