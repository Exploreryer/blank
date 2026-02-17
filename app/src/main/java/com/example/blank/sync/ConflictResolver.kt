package com.example.blank.sync

import com.example.blank.data.local.NoteEntity
import com.example.blank.data.remote.drive.RemoteNote
import com.example.blank.domain.model.SyncState
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
