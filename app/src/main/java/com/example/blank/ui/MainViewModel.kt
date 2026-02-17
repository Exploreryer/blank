package com.example.blank.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blank.BlankApp
import com.example.blank.data.local.NoteEntity
import com.example.blank.data.repository.sha256
import com.example.blank.domain.model.SyncState
import com.example.blank.sync.SyncPreferences
import com.example.blank.sync.SubscriptionPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

enum class DrawerTab { CURRENT, ARCHIVED }
enum class Destination { EDITOR, SETTINGS, ABOUT }

data class MainUiState(
    val selectedNoteId: String? = null,
    val selectedTab: DrawerTab = DrawerTab.CURRENT,
    val destination: Destination = Destination.EDITOR,
    val isProEnabled: Boolean = false,
    val isSyncEnabled: Boolean = true,
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null
)

@Serializable
private data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val notes: List<BackupNote>
)

@Serializable
private data class BackupNote(
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isStarred: Boolean,
    val isArchived: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as BlankApp).container
    private val repository = appContainer.noteRepository
    private val authManager = appContainer.authManager
    private val syncScheduler = appContainer.syncScheduler
    private val syncPreferences = SyncPreferences(application)
    private val subscriptionPreferences = SubscriptionPreferences(application)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private var saveJob: Job? = null
    private val draftContent = MutableStateFlow("")

    private val state = MutableStateFlow(
        MainUiState(
            isProEnabled = subscriptionPreferences.isProEnabled(),
            isSyncEnabled = syncPreferences.isSyncEnabled(),
            isSignedIn = authManager.isSignedIn(),
            signedInEmail = authManager.signedInEmail()
        )
    )
    val uiState = state.asStateFlow()

    val notes: StateFlow<Pair<List<NoteEntity>, List<NoteEntity>>> = combine(
        repository.observeCurrentNotes(),
        repository.observeArchivedNotes()
    ) { current, archived -> current to archived }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<NoteEntity>() to emptyList())

    init {
        viewModelScope.launch {
            repository.getAllAliveNotes()
                .asSequence()
                .filter { it.content.isBlank() }
                .forEach { repository.delete(it.id) }
        }
    }

    fun onEditorChanged(content: String) {
        if (draftContent.value == content) return
        draftContent.value = content
        saveJob?.cancel()
        val noteId = state.value.selectedNoteId
        saveJob = viewModelScope.launch {
            delay(220)
            if (noteId == null) {
                if (content.isBlank()) return@launch
                val note = repository.createNoteWithContent(content)
                state.value = state.value.copy(selectedNoteId = note.id)
                return@launch
            }
            if (content.isBlank()) {
                repository.delete(noteId)
                draftContent.value = ""
                state.value = state.value.copy(selectedNoteId = null)
                return@launch
            }
            repository.updateContent(noteId, content)
        }
    }

    fun selectNote(note: NoteEntity) {
        state.value = state.value.copy(selectedNoteId = note.id, destination = Destination.EDITOR)
        draftContent.value = note.content
    }

    fun createFreshNote() {
        saveJob?.cancel()
        draftContent.value = ""
        state.value = state.value.copy(selectedNoteId = null, destination = Destination.EDITOR)
    }

    fun toggleStar(noteId: String) {
        viewModelScope.launch { repository.toggleStar(noteId) }
    }

    fun toggleArchive(noteId: String) {
        viewModelScope.launch { repository.toggleArchive(noteId) }
    }

    fun setArchived(noteId: String, archived: Boolean) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId) ?: return@launch
            if (note.isArchived != archived) {
                repository.toggleArchive(noteId)
            }
        }
    }

    fun delete(noteId: String) {
        viewModelScope.launch {
            repository.delete(noteId)
            if (state.value.selectedNoteId == noteId) {
                saveJob?.cancel()
                draftContent.value = ""
                state.value = state.value.copy(selectedNoteId = null, destination = Destination.EDITOR)
            }
        }
    }

    fun setTab(tab: DrawerTab) {
        state.value = state.value.copy(selectedTab = tab)
    }

    fun navigate(destination: Destination) {
        state.value = state.value.copy(destination = destination)
    }

    fun setSyncEnabled(enabled: Boolean) {
        if (enabled && (!state.value.isProEnabled || !state.value.isSignedIn)) {
            syncPreferences.setSyncEnabled(false)
            state.value = state.value.copy(isSyncEnabled = false)
            syncScheduler.cancelSync()
            return
        }
        syncPreferences.setSyncEnabled(enabled)
        state.value = state.value.copy(isSyncEnabled = enabled)
        if (enabled) syncScheduler.scheduleForegroundSync()
        else syncScheduler.cancelSync()
    }

    fun refreshAuthState() {
        state.value = state.value.copy(
            isSignedIn = authManager.isSignedIn(),
            signedInEmail = authManager.signedInEmail()
        )
    }

    fun triggerSyncNow() {
        if (!state.value.isProEnabled || !state.value.isSyncEnabled) return
        syncScheduler.scheduleForegroundSync()
    }

    fun enablePro() {
        if (state.value.isProEnabled) return
        subscriptionPreferences.setProEnabled(true)
        state.value = state.value.copy(isProEnabled = true)
        if (state.value.isSyncEnabled) {
            syncScheduler.schedulePeriodicSync()
            syncScheduler.scheduleForegroundSync()
        }
    }

    fun selectedNoteFrom(all: Pair<List<NoteEntity>, List<NoteEntity>>): NoteEntity? {
        val selected = state.value.selectedNoteId ?: return null
        return (all.first + all.second).firstOrNull { it.id == selected }
    }

    fun editorContent(selected: NoteEntity?): String {
        if (state.value.selectedNoteId == null) return draftContent.value
        if (selected == null || selected.id != state.value.selectedNoteId) return selected?.content.orEmpty()
        return draftContent.value
    }

    fun conflictCount(all: Pair<List<NoteEntity>, List<NoteEntity>>): Int {
        return (all.first + all.second).count { it.syncState == SyncState.CONFLICT }
    }

    suspend fun exportNotesForBackup(): String {
        val aliveNotes = repository.getAllAliveNotes()
        val payload = BackupPayload(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            notes = aliveNotes.map {
                BackupNote(
                    content = it.content,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isStarred = it.isStarred,
                    isArchived = it.isArchived
                )
            }
        )
        return json.encodeToString(payload)
    }

    suspend fun importNotesFromBackup(rawJson: String): Int {
        val payload = json.decodeFromString<BackupPayload>(rawJson)
        if (payload.notes.isEmpty()) return 0
        val imported = payload.notes.map {
            val normalizedUpdatedAt = maxOf(it.createdAt, it.updatedAt)
            NoteEntity(
                id = UUID.randomUUID().toString(),
                content = it.content,
                createdAt = it.createdAt,
                updatedAt = normalizedUpdatedAt,
                isStarred = it.isStarred,
                isArchived = it.isArchived,
                syncState = SyncState.PENDING,
                contentHash = sha256(it.content)
            )
        }
        repository.upsertAll(imported)
        triggerSyncNow()
        return imported.size
    }

}
