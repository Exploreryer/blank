package com.example.blank.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.blank.BlankApp
import com.example.blank.data.local.NoteEntity
import com.example.blank.domain.model.SyncState

class DriveSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BlankApp
        val authManager = app.container.authManager
        val repository = app.container.noteRepository
        val driveClient = app.container.driveClient
        val conflictResolver = app.container.conflictResolver
        val syncPreferences = SyncPreferences(applicationContext)
        val subscriptionPreferences = SubscriptionPreferences(applicationContext)

        if (!subscriptionPreferences.isProEnabled() || !syncPreferences.isSyncEnabled()) return Result.success()

        val accessToken = runCatching { authManager.getAccessToken() }.getOrNull() ?: return Result.success()

        return runCatching {
            val remoteNotes = driveClient.fetchAllNotes(accessToken).associateBy { it.noteId }
            val localNotes = repository.getAllAliveNotes().associateBy { it.id }

            localNotes.values.forEach { local ->
                val remote = remoteNotes[local.id]
                when {
                    remote == null && local.deletedAt == null -> {
                        val fileId = driveClient.upsertNote(accessToken, local)
                        repository.upsert(
                            local.copy(
                                driveFileId = fileId ?: local.driveFileId,
                                syncState = SyncState.SYNCED,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    remote != null && shouldCreateConflict(local, remote.updatedAt, remote.contentHash) -> {
                        val conflictCopy = conflictResolver.createConflictCopy(remote, local)
                        repository.upsert(conflictCopy)
                        repository.upsert(local.copy(syncState = SyncState.CONFLICT))
                    }

                    remote != null && local.updatedAt >= remote.updatedAt -> {
                        val fileId = driveClient.upsertNote(accessToken, local)
                        repository.upsert(
                            local.copy(
                                driveFileId = fileId ?: local.driveFileId,
                                syncState = SyncState.SYNCED,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            remoteNotes.values.forEach { remote ->
                if (localNotes[remote.noteId] == null) {
                    repository.upsert(
                        NoteEntity(
                            id = remote.noteId,
                            content = remote.content,
                            createdAt = remote.updatedAt,
                            updatedAt = remote.updatedAt,
                            isStarred = false,
                            isArchived = false,
                            syncState = SyncState.SYNCED,
                            driveFileId = remote.fileId,
                            contentHash = remote.contentHash,
                            lastSyncedAt = remote.updatedAt
                        )
                    )
                }
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    private fun shouldCreateConflict(local: NoteEntity, remoteUpdatedAt: Long, remoteHash: String): Boolean {
        val lastSyncedAt = local.lastSyncedAt ?: return false
        val localChanged = local.updatedAt > lastSyncedAt
        val remoteChanged = remoteUpdatedAt > lastSyncedAt
        return localChanged && remoteChanged && local.contentHash != remoteHash
    }
}
