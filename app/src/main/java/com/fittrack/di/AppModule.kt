package com.fittrack.di

import android.content.Context
import androidx.room.Room
import com.fittrack.ai.BodyScanAnalyzer
import com.fittrack.ai.MealPlanGenerator
import com.fittrack.ai.WorkoutPlanGenerator
import com.fittrack.data.local.FitTrackDatabase
import com.fittrack.data.local.dao.ExerciseDao
import com.fittrack.data.local.dao.MealDao
import com.fittrack.data.remote.FoodApiService
import com.fittrack.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ═══════════════════════════════════════════════════════════════
    // DATABASE
    // ═══════════════════════════════════════════════════════════════
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FitTrackDatabase {
        return Room.databaseBuilder(
            context,
            FitTrackDatabase::class.java,
            "fittrack.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideMealDao(database: FitTrackDatabase): MealDao {
        return database.mealDao()
    }
    
    @Provides
    fun provideExerciseDao(database: FitTrackDatabase): ExerciseDao {
        return database.exerciseDao()
    }
    
    // ═══════════════════════════════════════════════════════════════
    // NETWORKING
    // ═══════════════════════════════════════════════════════════════
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideFoodApiService(okHttpClient: OkHttpClient): FoodApiService {
        return Retrofit.Builder()
            .baseUrl(FoodApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodApiService::class.java)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // REPOSITORIES
    // ═══════════════════════════════════════════════════════════════
    
    @Provides
    @Singleton
    fun provideMealRepository(mealDao: MealDao): MealRepository {
        return MealRepositoryImpl(mealDao)
    }
    
    @Provides
    @Singleton
    fun provideExerciseRepository(exerciseDao: ExerciseDao): ExerciseRepository {
        return ExerciseRepositoryImpl(exerciseDao)
    }
    
    @Provides
    @Singleton
    fun provideWorkoutRepository(exerciseDao: ExerciseDao): WorkoutRepository {
        return WorkoutRepositoryImpl(exerciseDao)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // AI ENGINES
    // ═══════════════════════════════════════════════════════════════
    
    @Provides
    @Singleton
    fun provideMealPlanGenerator(): MealPlanGenerator {
        return MealPlanGenerator()
    }
    
    @Provides
    @Singleton
    fun provideWorkoutPlanGenerator(): WorkoutPlanGenerator {
        return WorkoutPlanGenerator()
    }
    
    @Provides
    @Singleton
    fun provideBodyScanAnalyzer(@ApplicationContext context: Context): BodyScanAnalyzer {
        return BodyScanAnalyzer(context)
    }
}
