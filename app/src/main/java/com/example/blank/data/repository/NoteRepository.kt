package com.example.blank.data.repository

import com.example.blank.data.local.NoteDao
import com.example.blank.data.local.NoteEntity
import com.example.blank.domain.model.SyncState
import com.example.blank.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class NoteRepository(
    private val noteDao: NoteDao,
    private val syncScheduler: SyncScheduler
) {
    @Volatile
    private var lastForegroundSyncAt = 0L

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

    suspend fun createNoteWithContent(content: String): NoteEntity {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            content = content,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
            contentHash = sha256(content)
        )
        noteDao.insert(note)
        scheduleForegroundSync(force = false)
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
        scheduleForegroundSync(force = false)
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
        scheduleForegroundSync(force = true)
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
        scheduleForegroundSync(force = true)
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
        scheduleForegroundSync(force = true)
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

    private fun scheduleForegroundSync(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastForegroundSyncAt < 1_500L) {
            return
        }
        lastForegroundSyncAt = now
        syncScheduler.scheduleForegroundSync()
    }
}
