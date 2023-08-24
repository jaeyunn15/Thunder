package com.jeremy.thunder.ui.favorite

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable


fun NavGraphBuilder.favoriteNavGraph(navHostController: NavHostController, modifier: Modifier) {
    composable(
        route = "searchRoute"
    ) {
        FavoriteScreen()
    }
}