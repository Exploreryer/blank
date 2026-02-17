package com.example.quickdraft.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query(
        """
        SELECT * FROM notes 
        WHERE deletedAt IS NULL AND isArchived = 0
        ORDER BY isStarred DESC, updatedAt DESC
        """
    )
    fun observeCurrentNotes(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE deletedAt IS NULL AND isArchived = 1
        ORDER BY isStarred DESC, updatedAt DESC
        """
    )
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND syncState != 'SYNCED'")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    suspend fun getAllAliveNotes(): List<NoteEntity>
}
