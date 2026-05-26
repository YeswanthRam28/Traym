package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.ApiClient
import com.gymtracker.ui.screens.ProgressUiState
import com.gymtracker.ui.screens.StatItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchProgressData()
    }

    fun fetchProgressData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiClient.apiService.getWorkoutHistory()
                if (response.isSuccessful) {
                    val workouts = response.body() ?: emptyList()
                    
                    // 1. Calculate Strength Trend (Total Volume over last N workouts)
                    val trendPoints = workouts.takeLast(7).map { it.total_volume_kg ?: 0.0f }
                    
                    // 2. Aggregate Stats
                    val stats = listOf(
                        StatItem("TOTAL WORKOUTS", "${workouts.size}"),
                        StatItem("WEEKLY VOLUME", String.format("%.0f KG", trendPoints.sum())),
                        StatItem("AVG DURATION", "${(workouts.map { it.duration_seconds ?: 0 }.average() / 60).toInt()} MIN"),
                        StatItem("PRs THIS WEEK", "3") // Placeholder
                    )
                    
                    _uiState.update { 
                        it.copy(
                            strengthTrendPoints = trendPoints,
                            stats = stats,
                            plateauAlert = if (trendPoints.size >= 3 && trendPoints.last() <= trendPoints[trendPoints.size - 2]) 
                                "Volume is stabilizing. Consider increasing weight by 2.5% next session." 
                                else ""
                        )
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
