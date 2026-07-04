package com.aistudio.retroplay.wmpkqa.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Emulator : Screen("emulator/{gameId}") {
        fun createRoute(gameId: String) = "emulator/$gameId"
    }
}
