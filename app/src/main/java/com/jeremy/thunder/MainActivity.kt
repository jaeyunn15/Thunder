package com.jeremy.thunder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.jeremy.thunder.ui.theme.ThunderTheme
import com.jeremy.thunder.navGraph.Graph
import com.jeremy.thunder.navGraph.RootNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            ThunderTheme {
                RootNavGraph(
                    navController = navController,
                    startDestination = Graph.MAIN
                )
            }
        }
    }
}