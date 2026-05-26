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

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.apiService.getMe()
                if (response.isSuccessful) {
                    response.body()?.let { userProfile ->
                        _uiState.update {
                            it.copy(
                                userName = userProfile.name ?: "Unknown Athlete",
                                philosophy = userProfile.philosophy?.uppercase() ?: "NO PHILOSOPHY SET",
                                // We don't have streak/workouts/lbsLifted in UserProfileResponse yet, 
                                // so we keep them at "0" or update later when API expands.
                                streak = "0",
                                workouts = "0",
                                kgLifted = "0"
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
