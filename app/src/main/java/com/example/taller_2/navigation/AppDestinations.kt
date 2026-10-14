package com.example.taller_2.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Home : AppDestination

    @Serializable
    data object Camera : AppDestination

    @Serializable
    data object Map : AppDestination
}
