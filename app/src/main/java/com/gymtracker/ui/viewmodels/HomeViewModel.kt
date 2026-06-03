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
                val profileUrl = com.gymtracker.auth.SessionManager.getProfilePicUrl()
                _uiState.update { it.copy(profilePicUrl = profileUrl) }

                // 1. Fetch History for Volume
                val historyResponse = ApiClient.apiService.getWorkoutHistory()
                if (historyResponse.isSuccessful) {
                    val cal = java.util.Calendar.getInstance()
                    cal.firstDayOfWeek = java.util.Calendar.MONDAY
                    val currentWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
                    val currentYear = cal.get(java.util.Calendar.YEAR)
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

                    val weeklyWorkouts = historyResponse.body()?.filter { 
                        try {
                            val dateStr = it.started_at?.take(10) ?: ""
                            val date = format.parse(dateStr)
                            if (date != null) {
                                val c = java.util.Calendar.getInstance()
                                c.firstDayOfWeek = java.util.Calendar.MONDAY
                                c.time = date
                                c.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && c.get(java.util.Calendar.YEAR) == currentYear
                            } else false
                        } catch (e: Exception) { false }
                    } ?: emptyList()
                    
                    val totalVolume = weeklyWorkouts.sumOf { it.total_volume_kg?.toDouble() ?: 0.0 }
                    _uiState.update { it.copy(volume = String.format("%.0f", totalVolume)) }
                }

                // 2. Fetch PRs
                // Note: Need to add getPRs to TraymApiService
                val prResponse = ApiClient.apiService.getPRs()
                if (prResponse.isSuccessful) {
                    val prs = prResponse.body()?.map { 
                        PrItem(
                            name = it.exercise.uppercase(), 
                            weight = "${it.weight_kg.toInt()} KG",
                            isBigThree = it.is_big_three
                        )
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
