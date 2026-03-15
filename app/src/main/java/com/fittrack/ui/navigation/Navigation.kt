package com.fittrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fittrack.ui.screens.dashboard.DashboardScreen
import com.fittrack.ui.screens.nutrition.NutritionScreen
import com.fittrack.ui.screens.nutrition.AddMealScreen
import com.fittrack.ui.screens.nutrition.FoodSearchScreen
import com.fittrack.ui.screens.workout.WorkoutScreen
import com.fittrack.ui.screens.workout.ActiveWorkoutScreen
import com.fittrack.ui.screens.workout.ExercisePickerScreen
import com.fittrack.ui.screens.profile.ProfileScreen
import com.fittrack.ui.screens.profile.HealthConnectScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    object Nutrition : Screen(
        route = "nutrition",
        title = "Nutrition",
        selectedIcon = Icons.Filled.Restaurant,
        unselectedIcon = Icons.Outlined.Restaurant
    )
    
    object Workout : Screen(
        route = "workout",
        title = "Workout",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    )
    
    object Profile : Screen(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

// Additional routes not in bottom nav
object Routes {
    const val ADD_MEAL = "add_meal/{mealType}"
    const val FOOD_SEARCH = "food_search"
    const val FOOD_DETAIL = "food_detail/{foodId}"
    const val BARCODE_SCANNER = "barcode_scanner"
    const val ACTIVE_WORKOUT = "active_workout/{workoutId}"
    const val START_WORKOUT = "start_workout"
    const val EXERCISE_PICKER = "exercise_picker"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val WORKOUT_HISTORY = "workout_history"
    const val HEALTH_CONNECT = "health_connect"
    const val EDIT_GOALS = "edit_goals"
    
    fun addMeal(mealType: String) = "add_meal/$mealType"
    fun foodDetail(foodId: String) = "food_detail/$foodId"
    fun activeWorkout(workoutId: Long) = "active_workout/$workoutId"
    fun exerciseDetail(exerciseId: String) = "exercise_detail/$exerciseId"
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Nutrition,
    Screen.Workout,
    Screen.Profile
)

@Composable
fun FitTrackNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // Bottom nav destinations
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        
        composable(Screen.Nutrition.route) {
            NutritionScreen(navController = navController)
        }
        
        composable(Screen.Workout.route) {
            WorkoutScreen(navController = navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        
        // Nutrition sub-screens
        composable(
            route = Routes.ADD_MEAL,
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealType = backStackEntry.arguments?.getString("mealType") ?: "BREAKFAST"
            AddMealScreen(
                mealType = mealType,
                navController = navController
            )
        }
        
        composable(Routes.FOOD_SEARCH) {
            FoodSearchScreen(navController = navController)
        }
        
        // Workout sub-screens
        composable(Routes.START_WORKOUT) {
            ActiveWorkoutScreen(
                workoutId = null,
                navController = navController
            )
        }
        
        composable(
            route = Routes.ACTIVE_WORKOUT,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId")
            ActiveWorkoutScreen(
                workoutId = workoutId,
                navController = navController
            )
        }
        
        composable(Routes.EXERCISE_PICKER) {
            ExercisePickerScreen(navController = navController)
        }
        
        // Profile sub-screens
        composable(Routes.HEALTH_CONNECT) {
            HealthConnectScreen(navController = navController)
        }
    }
}
