package com.gymtracker.network

import android.util.Base64
import android.util.Log
import com.gymtracker.BuildConfig
import com.gymtracker.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotionOAuthManager {

    private val _authCodeFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    val authCodeFlow = _authCodeFlow.asSharedFlow()

    fun handleAuthCode(code: String) {
        _authCodeFlow.tryEmit(code)
    }

    suspend fun exchangeCodeForToken(code: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val clientId = BuildConfig.NOTION_CLIENT_ID
            val clientSecret = BuildConfig.NOTION_CLIENT_SECRET
            
            if (clientId.isEmpty() || clientSecret.isEmpty()) {
                return@withContext Result.failure(Exception("Notion Client ID or Secret is missing"))
            }

            val redirectUri = BuildConfig.NOTION_REDIRECT_URI
            val authString = "$clientId:$clientSecret"
            val base64Auth = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

            val url = URL("https://api.notion.com/v1/oauth/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Basic $base64Auth")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("grant_type", "authorization_code")
                put("code", code)
                put("redirect_uri", redirectUri)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonBody.toString())
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val accessToken = json.optString("access_token")
                val workspaceName = json.optString("workspace_name")
                
                SessionManager.saveNotionConfig(
                    token = accessToken,
                    databaseId = "", // We will set this after creating the DB
                    enabled = true
                )
                
                Result.success(Pair(accessToken, workspaceName))
            } else {
                val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("NotionOAuth", "Token exchange failed: $responseCode - $errorResponse")
                Result.failure(Exception("Failed to exchange code: $responseCode - $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("NotionOAuth", "Token exchange error", e)
            Result.failure(e)
        }
    }
}
