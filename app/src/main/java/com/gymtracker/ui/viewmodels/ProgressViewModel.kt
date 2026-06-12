package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.LocalStatsRepository
import com.gymtracker.data.ProgressStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProgressStats?>(null)
    val uiState: StateFlow<ProgressStats?> = _uiState.asStateFlow()

    private val _allPrs = MutableStateFlow<List<com.gymtracker.network.PrResponse>>(emptyList())
    val allPrs: StateFlow<List<com.gymtracker.network.PrResponse>> = _allPrs.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                LocalStatsRepository.getProgressStats()
            }
            _uiState.value = stats
            
            try {
                val prResponse = com.gymtracker.network.ApiClient.apiService.getPRs()
                if (prResponse.isSuccessful) {
                    _allPrs.value = prResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }
}
