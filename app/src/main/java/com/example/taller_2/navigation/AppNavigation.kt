package com.example.taller_2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taller_2.screens.CameraScreen
import com.example.taller_2.screens.HomeScreen
import com.example.taller_2.screens.MapScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home,
        modifier = modifier,
    ) {
        composable<AppDestination.Home> {
            HomeScreen(
                onNavigateToCamera = { navController.navigate(AppDestination.Camera) },
                onNavigateToMap = { navController.navigate(AppDestination.Map) },
            )
        }
        composable<AppDestination.Camera> {
            CameraScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable<AppDestination.Map> {
            MapScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
