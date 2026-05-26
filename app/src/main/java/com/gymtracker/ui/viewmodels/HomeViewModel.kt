package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.ApiClient
import com.gymtracker.ui.screens.HomeUiState
import com.gymtracker.ui.screens.PrItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Fetch History for Volume
                val historyResponse = ApiClient.apiService.getWorkoutHistory()
                if (historyResponse.isSuccessful) {
                    val totalVolume = historyResponse.body()?.sumOf { it.total_volume_kg?.toDouble() ?: 0.0 } ?: 0.0
                    _uiState.update { it.copy(volume = String.format("%.0f", totalVolume)) }
                }

                // 2. Fetch PRs
                // Note: Need to add getPRs to TraymApiService
                val prResponse = ApiClient.apiService.getPRs()
                if (prResponse.isSuccessful) {
                    val prs = prResponse.body()?.map { 
                        PrItem(name = it.exercise.uppercase(), weight = "${it.weight_kg.toInt()} KG")
                    } ?: emptyList()
                    _uiState.update { it.copy(recentPrs = prs) }
                }

                // 3. Fetch Plan for Today's Workout
                val planResponse = ApiClient.apiService.getActivePlan()
                if (planResponse.isSuccessful) {
                    val plan = planResponse.body()
                    if (plan != null) {
                        _uiState.update { 
                            it.copy(
                                todayWorkoutTitle = "ACTIVE PLAN: WEEK ${plan.week_number}",
                                todayWorkoutDesc = "Ready for your next session."
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
