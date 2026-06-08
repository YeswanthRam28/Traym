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
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_PROFILE_PIC_URL = "profile_pic_url"
    private const val KEY_USER_WEIGHT = "user_weight"
    private const val KEY_USER_HEIGHT = "user_height"

    private const val KEY_NOTION_TOKEN = "notion_token"
    private const val KEY_NOTION_DATABASE_ID = "notion_database_id"
    private const val KEY_NOTION_SYNC_ENABLED = "notion_sync_enabled"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    var authToken: String? = null
        set(value) {
            field = value
            if (::prefs.isInitialized) {
                if (value != null) {
                    prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
                } else {
                    prefs.edit().remove(KEY_AUTH_TOKEN).apply()
                }
            }
        }

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
        
        val savedToken = prefs.getString(KEY_AUTH_TOKEN, null)
        authToken = savedToken
        
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false) || savedToken != null
        _isLoggedIn.value = loggedIn
        
        onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setLoggedIn(loggedIn: Boolean, token: String? = null, userName: String? = null, userId: String? = null, profilePicUrl: String? = null, onboarded: Boolean = true) {
        _isLoggedIn.value = loggedIn
        authToken = token
        onboardingComplete = onboarded

        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            if (token != null) {
                putString(KEY_AUTH_TOKEN, token)
            } else {
                remove(KEY_AUTH_TOKEN)
            }
            if (userName != null) {
                putString(KEY_USER_NAME, userName)
            }
            if (userId != null) {
                putString(KEY_USER_ID, userId)
            } else if (!loggedIn) {
                remove(KEY_USER_ID)
            }
            if (profilePicUrl != null) {
                putString(KEY_PROFILE_PIC_URL, profilePicUrl)
            } else if (!loggedIn) {
                remove(KEY_PROFILE_PIC_URL)
            }
            putBoolean(KEY_ONBOARDING_COMPLETE, onboarded)
            apply()
        }
        
        // Update profile.json if userName is provided
        if (userName != null && ::appContext.isInitialized) {
            try {
                val file = java.io.File(appContext.filesDir, "profile.json")
                val json = if (file.exists()) {
                    org.json.JSONObject(file.readText())
                } else {
                    org.json.JSONObject().apply {
                        put("email", "athlete@example.com")
                        put("goal", "muscle hypertrophy")
                        put("philosophy", "hypertrophy")
                        put("onboarding_complete", true)
                    }
                }
                json.put("name", userName)
                file.writeText(json.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (token != null) {
            com.gymtracker.network.ApiClient.setAuthToken(token)
        }
    }

    fun getUserName(): String {
        return if (::prefs.isInitialized) {
            prefs.getString(KEY_USER_NAME, "Athlete") ?: "Athlete"
        } else {
            "Athlete"
        }
    }

    fun getUserWeight(): String = if (::prefs.isInitialized) prefs.getString(KEY_USER_WEIGHT, "") ?: "" else ""
    fun getUserHeight(): String = if (::prefs.isInitialized) prefs.getString(KEY_USER_HEIGHT, "") ?: "" else ""

    fun updateUserDetails(name: String, weight: String, height: String) {
        if (::prefs.isInitialized) {
            prefs.edit().apply {
                putString(KEY_USER_NAME, name)
                putString(KEY_USER_WEIGHT, weight)
                putString(KEY_USER_HEIGHT, height)
                apply()
            }
            
            // Also update profile.json for api compatibility
            try {
                val file = java.io.File(appContext.filesDir, "profile.json")
                val json = if (file.exists()) {
                    org.json.JSONObject(file.readText())
                } else {
                    org.json.JSONObject().apply {
                        put("email", "athlete@example.com")
                        put("goal", "muscle hypertrophy")
                        put("philosophy", "hypertrophy")
                        put("onboarding_complete", true)
                    }
                }
                json.put("name", name)
                json.put("weight", weight)
                json.put("height", height)
                file.writeText(json.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getUserId(): String {
        return if (::prefs.isInitialized) {
            var id = prefs.getString(KEY_USER_ID, null)
            if (id == null) {
                id = java.util.UUID.randomUUID().toString()
                prefs.edit().putString(KEY_USER_ID, id).apply()
            }
            id
        } else {
            "anonymous"
        }
    }

    fun getProfilePicUrl(): String? {
        return if (::prefs.isInitialized) {
            prefs.getString(KEY_PROFILE_PIC_URL, null)
        } else {
            null
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
            _isLoggedIn.value = false
            authToken = null
            onboardingComplete = false
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
