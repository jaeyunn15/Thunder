package com.jeremy.thunder.navGraph

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jeremy.thunder.ui.favorite.favoriteNavGraph
import com.jeremy.thunder.ui.home.homeNavGraph

fun NavGraphBuilder.mainNavGraph() {
    composable(
        route = Graph.MAIN
    ) {
        MainScreen()
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavigation(navController = navController) }
    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            MainNavGraph(
                navController = navController
            )
        }
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarDestination = items.any { it.screenRoute == currentDestination?.route }

    if (bottomBarDestination) {
        BottomNavigation(
            backgroundColor = Color.White,
            contentColor = Color(0xFF3F414E),
            modifier = Modifier.height(bottomBarHeight)
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.screenRoute } == true
                BottomNavigationItem(
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = stringResource(id = item.title),
                            modifier = Modifier
                                .width(25.dp)
                                .height(25.dp)
                        )
                    },
                    label = { Text(stringResource(id = item.title), fontSize = 9.sp) },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = Color.Gray,
                    selected = selected,
                    onClick = {
                        navController.navigate(item.screenRoute) {
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.screenRoute
    ) {
        val modifier = Modifier.padding(bottom = bottomBarHeight)
        homeNavGraph(navController, modifier)
        favoriteNavGraph(navController, modifier)
    }
}

val bottomBarHeight = 60.dp