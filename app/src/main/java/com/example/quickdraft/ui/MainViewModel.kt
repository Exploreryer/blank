package com.example.quickdraft.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickdraft.BlankApp
import com.example.quickdraft.data.local.NoteEntity
import com.example.quickdraft.domain.model.SyncState
import com.example.quickdraft.sync.SyncPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DrawerTab { CURRENT, ARCHIVED }
enum class Destination { EDITOR, SETTINGS }

data class MainUiState(
    val selectedNoteId: String? = null,
    val selectedTab: DrawerTab = DrawerTab.CURRENT,
    val destination: Destination = Destination.EDITOR,
    val isSyncEnabled: Boolean = true,
    val isSignedIn: Boolean = false,
    val signedInEmail: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as BlankApp).container
    private val repository = appContainer.noteRepository
    private val authManager = appContainer.authManager
    private val syncScheduler = appContainer.syncScheduler
    private val syncPreferences = SyncPreferences(application)
    private var saveJob: Job? = null
    private val draftContent = MutableStateFlow("")

    private val state = MutableStateFlow(
        MainUiState(
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
            val note = repository.createEmptyNote()
            draftContent.value = note.content
            state.value = state.value.copy(selectedNoteId = note.id)
        }
    }

    fun onEditorChanged(content: String) {
        val noteId = state.value.selectedNoteId ?: return
        if (draftContent.value == content) return
        draftContent.value = content
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(120)
            repository.updateContent(noteId, content)
        }
    }

    fun selectNote(note: NoteEntity) {
        state.value = state.value.copy(selectedNoteId = note.id, destination = Destination.EDITOR)
        draftContent.value = note.content
    }

    fun createFreshNote() {
        viewModelScope.launch {
            val note = repository.createEmptyNote()
            draftContent.value = note.content
            state.value = state.value.copy(selectedNoteId = note.id, destination = Destination.EDITOR)
        }
    }

    fun toggleStar(noteId: String) {
        viewModelScope.launch { repository.toggleStar(noteId) }
    }

    fun toggleArchive(noteId: String) {
        viewModelScope.launch { repository.toggleArchive(noteId) }
    }

    fun delete(noteId: String) {
        viewModelScope.launch {
            repository.delete(noteId)
            if (state.value.selectedNoteId == noteId) {
                createFreshNote()
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
        syncPreferences.setSyncEnabled(enabled)
        state.value = state.value.copy(isSyncEnabled = enabled)
        if (enabled) syncScheduler.scheduleForegroundSync()
    }

    fun refreshAuthState() {
        state.value = state.value.copy(
            isSignedIn = authManager.isSignedIn(),
            signedInEmail = authManager.signedInEmail()
        )
    }

    fun triggerSyncNow() {
        syncScheduler.scheduleForegroundSync()
    }

    fun selectedNoteFrom(all: Pair<List<NoteEntity>, List<NoteEntity>>): NoteEntity? {
        val selected = state.value.selectedNoteId ?: return null
        return (all.first + all.second).firstOrNull { it.id == selected }
    }

    fun editorContent(selected: NoteEntity?): String {
        if (selected == null || selected.id != state.value.selectedNoteId) return selected?.content.orEmpty()
        return draftContent.value
    }

    fun conflictCount(all: Pair<List<NoteEntity>, List<NoteEntity>>): Int {
        return (all.first + all.second).count { it.syncState == SyncState.CONFLICT }
    }

}
