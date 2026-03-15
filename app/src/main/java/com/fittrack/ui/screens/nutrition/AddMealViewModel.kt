package com.fittrack.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.data.remote.FoodApiService
import com.fittrack.data.remote.toFoodItem
import com.fittrack.data.repository.MealRepository
import com.fittrack.domain.model.FoodItem
import com.fittrack.domain.model.MealEntry
import com.fittrack.domain.model.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class AddMealUiState(
    val isLoading: Boolean = false,
    val mealType: MealType = MealType.BREAKFAST,
    val searchResults: List<FoodItem> = emptyList(),
    val recentFoods: List<FoodItem> = emptyList(),
    val customFoods: List<FoodItem> = emptyList(),
    val showRecent: Boolean = true,
    val showCustom: Boolean = false,
    val error: String? = null,
    val mealAdded: Boolean = false
)

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val foodApiService: FoodApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddMealUiState())
    val uiState: StateFlow<AddMealUiState> = _uiState.asStateFlow()
    
    private var currentQuery = ""
    
    init {
        loadRecentFoods()
    }
    
    fun setMealType(mealType: String) {
        _uiState.update { 
            it.copy(mealType = MealType.valueOf(mealType)) 
        }
    }
    
    private fun loadRecentFoods() {
        viewModelScope.launch {
            mealRepository.getRecentFoodItems(20).collect { foods ->
                _uiState.update { 
                    it.copy(recentFoods = foods)
                }
                
                // Show recent as default results if no search
                if (currentQuery.isEmpty() && _uiState.value.showRecent) {
                    _uiState.update { it.copy(searchResults = foods) }
                }
            }
        }
    }
    
    fun search(query: String) {
        currentQuery = query
        
        if (query.isEmpty()) {
            _uiState.update { it.copy(searchResults = it.recentFoods) }
            return
        }
        
        viewModelScope.launch {
            mealRepository.searchFoodItems(query).collect { foods ->
                _uiState.update { 
                    it.copy(searchResults = foods) 
                }
            }
        }
    }
    
    fun searchOnline(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val response = foodApiService.searchProducts(query)
                val foods = response.products?.mapNotNull { it.toFoodItem() } ?: emptyList()
                
                // Save to local database for future use
                foods.forEach { food ->
                    mealRepository.insertFoodItem(food)
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        searchResults = foods
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to search online: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun scanBarcode(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // First check local database
            val localFood = mealRepository.getFoodItemByBarcode(barcode)
            if (localFood != null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        searchResults = listOf(localFood)
                    ) 
                }
                return@launch
            }
            
            // Search online
            try {
                val response = foodApiService.getProductByBarcode(barcode)
                val food = response.product?.toFoodItem()
                
                if (food != null) {
                    mealRepository.insertFoodItem(food)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            searchResults = listOf(food)
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Product not found"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to scan barcode: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun toggleRecent() {
        _uiState.update { it.copy(showRecent = !it.showRecent, showCustom = false) }
        if (_uiState.value.showRecent && currentQuery.isEmpty()) {
            _uiState.update { it.copy(searchResults = it.recentFoods) }
        }
    }
    
    fun toggleCustom() {
        _uiState.update { it.copy(showCustom = !it.showCustom, showRecent = false) }
        if (_uiState.value.showCustom) {
            viewModelScope.launch {
                mealRepository.getCustomFoodItems().collect { foods ->
                    _uiState.update { 
                        it.copy(
                            customFoods = foods,
                            searchResults = foods
                        ) 
                    }
                }
            }
        }
    }
    
    fun addMealEntry(food: FoodItem, servings: Float, notes: String? = null) {
        viewModelScope.launch {
            val entry = MealEntry(
                foodItem = food,
                servings = servings,
                mealType = _uiState.value.mealType,
                loggedAt = LocalDateTime.now(),
                notes = notes
            )
            
            mealRepository.insertMealEntry(entry)
            
            _uiState.update { it.copy(mealAdded = true) }
        }
    }
    
    fun createCustomFood(food: FoodItem) {
        viewModelScope.launch {
            mealRepository.insertFoodItem(food.copy(isCustom = true))
            
            // Add to search results
            _uiState.update { state ->
                state.copy(
                    searchResults = listOf(food) + state.searchResults,
                    customFoods = listOf(food) + state.customFoods
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetMealAdded() {
        _uiState.update { it.copy(mealAdded = false) }
    }
}
