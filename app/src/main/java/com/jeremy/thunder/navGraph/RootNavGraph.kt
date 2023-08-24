package com.jeremy.thunder.navGraph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun RootNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        route = Graph.ROOT,
        startDestination = startDestination
    ) {
        mainNavGraph()
    }
}

object Graph {
    const val ROOT = "root_graph"
    const val MAIN = "main_graph"
}