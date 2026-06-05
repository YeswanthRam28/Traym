package com.gymtracker.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.ApiClient
import com.gymtracker.ui.screens.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.gymtracker.network.NotionOAuthManager
import com.gymtracker.network.NotionSyncManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _notionSetupStatus = MutableStateFlow<String?>(null)
    val notionSetupStatus: StateFlow<String?> = _notionSetupStatus.asStateFlow()

    init {
        fetchProfile()
        listenForNotionAuth()
    }

    private fun listenForNotionAuth() {
        viewModelScope.launch {
            NotionOAuthManager.authCodeFlow.collectLatest { code ->
                _notionSetupStatus.value = "Authenticating with Notion..."
                val result = NotionOAuthManager.exchangeCodeForToken(code)
                result.onSuccess { (token, workspaceName) ->
                    _notionSetupStatus.value = "Creating database in $workspaceName..."
                    NotionSyncManager.createDatabase(token) { success, msg ->
                        if (success) {
                            _notionSetupStatus.value = null
                            _successMessage.value = "Successfully connected to Notion!"
                        } else {
                            _notionSetupStatus.value = null
                            _errorMessage.value = "Failed to create Notion database: $msg"
                        }
                    }
                }.onFailure { e ->
                    _notionSetupStatus.value = null
                    _errorMessage.value = "Notion Auth Failed: ${e.message}"
                }
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.apiService.getMe()
                if (response.isSuccessful) {
                    response.body()?.let { userProfile ->
                        val historyResponse = ApiClient.apiService.getWorkoutHistory()
                        var totalWorkouts = 0
                        var totalVolume = 0.0
                        var streak = 0
                        
                        if (historyResponse.isSuccessful) {
                            val workouts = historyResponse.body() ?: emptyList()
                            totalWorkouts = workouts.size
                            totalVolume = workouts.sumOf { it.total_volume_kg?.toDouble() ?: 0.0 }
                            
                            if (workouts.isNotEmpty()) {
                                val cal = java.util.Calendar.getInstance()
                                cal.firstDayOfWeek = java.util.Calendar.MONDAY
                                val currentWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
                                val currentYear = cal.get(java.util.Calendar.YEAR)
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                
                                val activeDays = workouts.mapNotNull { 
                                    try {
                                        val dateStr = it.started_at?.take(10) ?: ""
                                        val date = format.parse(dateStr)
                                        if (date != null) {
                                            format.format(date)
                                        } else null
                                    } catch(e:Exception) { null }
                                }.toSet()
                                
                                var tempStreak = 0
                                val c = java.util.Calendar.getInstance()
                                
                                var checkDateStr = format.format(c.time)
                                if (!activeDays.contains(checkDateStr)) {
                                    c.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                    checkDateStr = format.format(c.time)
                                }
                                
                                while (true) {
                                    val isSunday = c.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
                                    
                                    if (activeDays.contains(checkDateStr)) {
                                        tempStreak++
                                        c.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                        checkDateStr = format.format(c.time)
                                    } else if (isSunday) {
                                        // Sunday is a free rest day. Don't break the streak, just move to Saturday.
                                        c.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                        checkDateStr = format.format(c.time)
                                    } else {
                                        break
                                    }
                                }
                                streak = tempStreak
                            }
                        }

                        _uiState.update {
                            it.copy(
                                userName = userProfile.name ?: "Unknown Athlete",
                                philosophy = userProfile.philosophy?.uppercase() ?: "NO PHILOSOPHY SET",
                                streak = streak.toString(),
                                workouts = totalWorkouts.toString(),
                                kgLifted = String.format("%.0f", totalVolume)
                            )
                        }
                    }
                } else {
                    _errorMessage.value = "Failed to fetch profile: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadExportFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Copy URI contents to a temporary file
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "chatgpt_export.json")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                val requestFile = tempFile.asRequestBody("application/json".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

                val response = ApiClient.apiService.uploadChatGPTExport(body)
                if (response.isSuccessful) {
                    _successMessage.value = "Import queued! You'll receive a notification when it's ready."
                } else {
                    _errorMessage.value = "Failed to upload file: ${response.code()}"
                }
                
                // Cleanup temp file
                tempFile.delete()

            } catch (e: Exception) {
                _errorMessage.value = "Upload error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
