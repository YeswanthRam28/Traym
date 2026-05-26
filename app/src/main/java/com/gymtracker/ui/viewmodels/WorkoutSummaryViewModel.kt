package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.ApiClient
import com.gymtracker.ui.screens.WorkoutSummaryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutSummaryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchLastWorkoutSummary()
    }

    fun fetchLastWorkoutSummary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.apiService.getWorkoutHistory()
                if (response.isSuccessful) {
                    val lastWorkout = response.body()?.firstOrNull()
                    if (lastWorkout != null) {
                        _uiState.update { 
                            it.copy(
                                workoutTitle = lastWorkout.title.uppercase(),
                                exercisesCount = "5", // Simplified
                                duration = "${(lastWorkout.duration_seconds ?: 0) / 60}m",
                                volume = String.format("%.0f", lastWorkout.total_volume_kg ?: 0.0f),
                                newPrsCount = "1", // Simplified
                                prCalloutTitle = "BENCH PRESS PR",
                                prCalloutValue = "100 KG"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
