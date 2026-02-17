package com.example.quickdraft.data.repository

import com.example.quickdraft.data.local.NoteDao
import com.example.quickdraft.data.local.NoteEntity
import com.example.quickdraft.domain.model.SyncState
import com.example.quickdraft.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class NoteRepository(
    private val noteDao: NoteDao,
    private val syncScheduler: SyncScheduler
) {
    fun observeCurrentNotes(): Flow<List<NoteEntity>> = noteDao.observeCurrentNotes()
    fun observeArchivedNotes(): Flow<List<NoteEntity>> = noteDao.observeArchivedNotes()

    suspend fun createEmptyNote(): NoteEntity {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            content = "",
            createdAt = now,
            updatedAt = now,
            contentHash = sha256("")
        )
        noteDao.insert(note)
        return note
    }

    suspend fun getNoteById(id: String): NoteEntity? = noteDao.getById(id)

    suspend fun updateContent(noteId: String, content: String) {
        val existing = noteDao.getById(noteId) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            content = content,
            updatedAt = now,
            syncState = SyncState.PENDING,
            contentHash = sha256(content)
        )
        noteDao.update(updated)
        syncScheduler.scheduleForegroundSync()
    }

    suspend fun toggleStar(noteId: String) {
        val existing = noteDao.getById(noteId) ?: return
        noteDao.update(
            existing.copy(
                isStarred = !existing.isStarred,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
        )
        syncScheduler.scheduleForegroundSync()
    }

    suspend fun toggleArchive(noteId: String) {
        val existing = noteDao.getById(noteId) ?: return
        noteDao.update(
            existing.copy(
                isArchived = !existing.isArchived,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
        )
        syncScheduler.scheduleForegroundSync()
    }

    suspend fun delete(noteId: String) {
        val existing = noteDao.getById(noteId) ?: return
        noteDao.update(
            existing.copy(
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
        )
        syncScheduler.scheduleForegroundSync()
    }

    suspend fun upsert(note: NoteEntity) = noteDao.insert(note)

    suspend fun upsertAll(notes: List<NoteEntity>) = noteDao.insertAll(notes)

    suspend fun getPendingSyncNotes(): List<NoteEntity> = noteDao.getPendingSyncNotes()

    suspend fun getAllAliveNotes(): List<NoteEntity> = noteDao.getAllAliveNotes()

    suspend fun ensureLaunchBlankNote(): NoteEntity {
        val latest = observeCurrentNotes().firstOrNull()?.firstOrNull()
        return if (latest != null && latest.content.isBlank()) {
            latest
        } else {
            createEmptyNote()
        }
    }
}
