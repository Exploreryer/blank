package com.example.blank.sync

import android.content.Context

class SubscriptionPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("blank_prefs", Context.MODE_PRIVATE)

    fun isProEnabled(): Boolean = preferences.getBoolean(KEY_PRO_ENABLED, false)

    fun setProEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PRO_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_PRO_ENABLED = "pro_enabled"
    }
}
