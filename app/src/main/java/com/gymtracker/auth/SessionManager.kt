package com.gymtracker.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SessionManager {
    private const val PREFS_NAME = "traym_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    private const val KEY_NOTION_TOKEN = "notion_token"
    private const val KEY_NOTION_DATABASE_ID = "notion_database_id"
    private const val KEY_NOTION_SYNC_ENABLED = "notion_sync_enabled"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(true) // Always logged in
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    var authToken: String? = "local_dev_token" // Always authorized

    var onboardingComplete: Boolean = false
        set(value) {
            field = value
            if (::prefs.isInitialized) {
                prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()
            }
        }

    lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isLoggedIn.value = true
        authToken = "local_dev_token"
        onboardingComplete = true // Bypassed onboarding
    }

    fun setLoggedIn(loggedIn: Boolean, token: String? = null, onboarded: Boolean = false) {
        _isLoggedIn.value = true // Ignore logouts from views
        authToken = "local_dev_token"
        onboardingComplete = true

        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_AUTH_TOKEN, "local_dev_token")
            putBoolean(KEY_ONBOARDING_COMPLETE, onboarded)
            apply()
        }
    }

    fun getNotionToken(): String = if (::prefs.isInitialized) prefs.getString(KEY_NOTION_TOKEN, "") ?: "" else ""
    fun getNotionDatabaseId(): String = if (::prefs.isInitialized) prefs.getString(KEY_NOTION_DATABASE_ID, "") ?: "" else ""
    fun isNotionSyncEnabled(): Boolean = if (::prefs.isInitialized) prefs.getBoolean(KEY_NOTION_SYNC_ENABLED, false) else false

    fun saveNotionConfig(token: String, databaseId: String, enabled: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit().apply {
                putString(KEY_NOTION_TOKEN, token.trim())
                putString(KEY_NOTION_DATABASE_ID, databaseId.trim())
                putBoolean(KEY_NOTION_SYNC_ENABLED, enabled)
                apply()
            }
        }
    }

    fun clearLocalData() {
        if (::prefs.isInitialized) {
            prefs.edit().clear().apply()
            _isLoggedIn.value = true
            authToken = "local_dev_token"
            onboardingComplete = true
        }
        // Delete local JSON files
        try {
            val dir = appContext.filesDir
            dir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
