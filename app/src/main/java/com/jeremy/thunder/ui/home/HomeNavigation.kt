package com.jeremy.thunder.ui.home

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable


fun NavGraphBuilder.homeNavGraph(navHostController: NavHostController, modifier: Modifier) {
    composable(
        route = "homeRoute"
    ) {
        HomeScreen()
    }
}