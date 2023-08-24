package com.jeremy.thunder.ui.favorite

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.jeremy.thunder.OnLifecycleEvent

@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.requestSpecificTicker()
        if (event == Lifecycle.Event.ON_PAUSE) viewModel.requestCancelSpecificTicker()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
    }
}