package com.fittrack.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts API service
 * Documentation: https://world.openfoodfacts.org/data
 */
interface FoodApiService {
    
    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = DEFAULT_FIELDS
    ): ProductResponse
    
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = DEFAULT_FIELDS
    ): SearchResponse
    
    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val DEFAULT_FIELDS = "code,product_name,brands,serving_size,nutriments,image_front_small_url"
    }
}

// Response models
data class ProductResponse(
    val code: String?,
    val status: Int?,
    val product: ProductDto?
)

data class SearchResponse(
    val count: Int?,
    val page: Int?,
    val page_size: Int?,
    val products: List<ProductDto>?
)

data class ProductDto(
    val code: String?,
    val product_name: String?,
    val brands: String?,
    val serving_size: String?,
    val nutriments: NutrimentsDto?,
    val image_front_small_url: String?
)

data class NutrimentsDto(
    // Per 100g values
    val energy_kcal_100g: Float?,
    val proteins_100g: Float?,
    val carbohydrates_100g: Float?,
    val fat_100g: Float?,
    val fiber_100g: Float?,
    val sugars_100g: Float?,
    val sodium_100g: Float?,
    // Per serving values (preferred if available)
    val energy_kcal_serving: Float?,
    val proteins_serving: Float?,
    val carbohydrates_serving: Float?,
    val fat_serving: Float?,
    val fiber_serving: Float?,
    val sugars_serving: Float?,
    val sodium_serving: Float?
)

// Extension to convert DTO to domain model
fun ProductDto.toFoodItem(): com.fittrack.domain.model.FoodItem? {
    val name = product_name ?: return null
    
    // Parse serving size (e.g., "100g", "1 cup (240ml)")
    val servingInfo = parseServingSize(serving_size)
    
    val nutrition = com.fittrack.domain.model.NutritionInfo(
        calories = (nutriments?.energy_kcal_serving ?: nutriments?.energy_kcal_100g ?: 0f).toInt(),
        protein = nutriments?.proteins_serving ?: nutriments?.proteins_100g ?: 0f,
        carbs = nutriments?.carbohydrates_serving ?: nutriments?.carbohydrates_100g ?: 0f,
        fat = nutriments?.fat_serving ?: nutriments?.fat_100g ?: 0f,
        fiber = nutriments?.fiber_serving ?: nutriments?.fiber_100g ?: 0f,
        sugar = nutriments?.sugars_serving ?: nutriments?.sugars_100g ?: 0f,
        sodium = ((nutriments?.sodium_serving ?: nutriments?.sodium_100g ?: 0f) * 1000) // g to mg
    )
    
    return com.fittrack.domain.model.FoodItem(
        id = "off_${code ?: System.currentTimeMillis()}",
        name = name,
        brand = brands,
        servingSize = servingInfo.first,
        servingUnit = servingInfo.second,
        nutrition = nutrition,
        barcode = code,
        imageUrl = image_front_small_url,
        isCustom = false
    )
}

private fun parseServingSize(servingSize: String?): Pair<Float, String> {
    if (servingSize.isNullOrBlank()) {
        return Pair(100f, "g")
    }
    
    // Try to extract number and unit
    val regex = Regex("""(\d+(?:\.\d+)?)\s*(\w+)?""")
    val match = regex.find(servingSize)
    
    return if (match != null) {
        val size = match.groupValues[1].toFloatOrNull() ?: 100f
        val unit = match.groupValues.getOrNull(2)?.lowercase() ?: "g"
        Pair(size, unit)
    } else {
        Pair(100f, "g")
    }
}
