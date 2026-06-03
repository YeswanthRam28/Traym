package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityViewModel : ViewModel() {
    private val repository = CommunityRepository()
    
    val leaderboard = repository.leaderboard

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            repository.fetchLeaderboard()
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.fetchLeaderboard()
            _isRefreshing.value = false
        }
    }
}
