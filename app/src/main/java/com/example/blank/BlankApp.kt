package com.example.blank

import android.app.Application
import com.example.blank.auth.GoogleAuthManager
import com.example.blank.data.local.AppDatabase
import com.example.blank.data.remote.drive.DriveClient
import com.example.blank.data.repository.NoteRepository
import com.example.blank.sync.ConflictResolver
import com.example.blank.sync.SyncScheduler

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
