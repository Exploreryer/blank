package com.example.quickdraft.sync

import android.content.Context

class SyncPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("blank_prefs", Context.MODE_PRIVATE)

    fun isSyncEnabled(): Boolean = preferences.getBoolean(KEY_SYNC_ENABLED, true)

    fun setSyncEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_SYNC_ENABLED = "sync_enabled"
    }
}
