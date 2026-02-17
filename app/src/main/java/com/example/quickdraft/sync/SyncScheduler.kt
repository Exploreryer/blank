package com.example.quickdraft.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleForegroundSync() {
        val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "drive_sync_now",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "drive_sync_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
