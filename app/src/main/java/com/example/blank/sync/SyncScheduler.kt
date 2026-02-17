package com.example.blank.sync

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
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(context)
    private val syncPreferences = SyncPreferences(appContext)
    private val subscriptionPreferences = SubscriptionPreferences(appContext)

    private fun canSync(): Boolean {
        return subscriptionPreferences.isProEnabled() && syncPreferences.isSyncEnabled()
    }

    fun scheduleForegroundSync() {
        if (!canSync()) {
            cancelSync()
            return
        }
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
        if (!canSync()) {
            cancelSync()
            return
        }
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

    fun cancelSync() {
        workManager.cancelUniqueWork("drive_sync_now")
        workManager.cancelUniqueWork("drive_sync_periodic")
    }
}
