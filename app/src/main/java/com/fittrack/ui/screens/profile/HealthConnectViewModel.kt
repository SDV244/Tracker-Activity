package com.fittrack.ui.screens.profile

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.health.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HealthConnectUiState(
    val isHealthConnectAvailable: Boolean = false,
    val hasPermissions: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val connectedApps: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()
    
    init {
        checkAvailability()
    }
    
    private fun checkAvailability() {
        val status = HealthConnectManager.isAvailable(context)
        val isAvailable = status == HealthConnectClient.SDK_AVAILABLE
        
        _uiState.update { it.copy(isHealthConnectAvailable = isAvailable) }
        
        if (isAvailable) {
            checkPermissions()
        }
    }
    
    private fun checkPermissions() {
        viewModelScope.launch {
            val hasPermissions = healthConnectManager.hasAllPermissions()
            _uiState.update { it.copy(hasPermissions = hasPermissions) }
            
            if (hasPermissions) {
                syncNow()
            }
        }
    }
    
    fun getPermissionContract(): ActivityResultContract<Set<String>, Set<String>> {
        return healthConnectManager.requestPermissionsActivityContract()
    }
    
    fun onPermissionResult(granted: Set<String>) {
        val hasAll = granted.containsAll(HealthConnectManager.PERMISSIONS)
        _uiState.update { it.copy(hasPermissions = hasAll) }
        
        if (hasAll) {
            syncNow()
        }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            
            try {
                // Perform sync
                val healthData = healthConnectManager.getHealthDataForDate(
                    java.time.LocalDate.now()
                )
                
                val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
                val syncTime = LocalDateTime.now().format(formatter)
                
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        lastSyncTime = syncTime
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        error = "Sync failed: ${e.message}"
                    ) 
                }
            }
        }
    }
}
