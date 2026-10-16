package com.example.taller_2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.taller_2.screens.CameraScreen
import com.example.taller_2.screens.HomeScreen
import com.example.taller_2.screens.MapScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    // En Navigation 3 la pila de navegación es una lista que controlamos nosotros.
    // rememberNavBackStack la guarda automáticamente (rotación / muerte del proceso).
    val backStack = rememberNavBackStack(AppDestination.Home)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // Botón/gesto "atrás" del sistema: quitar la última pantalla de la pila
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Guarda el estado de rememberSaveable de cada pantalla
            rememberSaveableStateHolderNavEntryDecorator(),
            // Hace que cada pantalla tenga su propio ViewModelStore:
            // el ViewModel vive mientras la pantalla esté en la pila y se destruye al salir
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppDestination.Home> {
                HomeScreen(
                    onNavigateToCamera = { backStack.add(AppDestination.Camera) },
                    onNavigateToMap = { backStack.add(AppDestination.Map) },
                )
            }
            entry<AppDestination.Camera> {
                CameraScreen(onBackClick = { backStack.removeLastOrNull() })
            }
            entry<AppDestination.Map> {
                MapScreen(onBackClick = { backStack.removeLastOrNull() })
            }
        },
    )
}
