package com.example.quickdraft

import android.app.Application
import com.example.quickdraft.auth.GoogleAuthManager
import com.example.quickdraft.data.local.AppDatabase
import com.example.quickdraft.data.remote.drive.DriveClient
import com.example.quickdraft.data.repository.NoteRepository
import com.example.quickdraft.sync.ConflictResolver
import com.example.quickdraft.sync.SyncScheduler

class BlankApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.syncScheduler.schedulePeriodicSync()
    }
}

class AppContainer(app: Application) {
    private val database = AppDatabase.getInstance(app)
    val authManager = GoogleAuthManager(app)
    val syncScheduler = SyncScheduler(app)
    val driveClient = DriveClient()
    val conflictResolver = ConflictResolver()
    val noteRepository = NoteRepository(
        noteDao = database.noteDao(),
        syncScheduler = syncScheduler
    )
}
