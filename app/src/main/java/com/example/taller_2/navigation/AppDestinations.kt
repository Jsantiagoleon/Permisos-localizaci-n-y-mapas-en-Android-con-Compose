package com.example.taller_2.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Destinos (rutas) de la app para Navigation 3.
 * Cada destino es una "llave" (NavKey) que se agrega o se quita de la pila (back stack).
 * Deben ser @Serializable para que la pila sobreviva a rotaciones y a la muerte del proceso.
 */
@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Home : AppDestination

    @Serializable
    data object Camera : AppDestination

    @Serializable
    data object Map : AppDestination
}
