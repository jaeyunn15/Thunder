package com.jeremy.thunder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.jeremy.thunder.ui.theme.ThunderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThunderTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(key1 = Unit) {
                        viewModel.request()
                    }

                    LaunchedEffect(key1 = Unit) {
                        viewModel.observeAllMarket()
                    }

                    LaunchedEffect(key1 = Unit) {
                        viewModel.observeTicker()
                    }

                    val state = viewModel.response.collectAsState()

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.value?.p.orEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = TextUnit.Unspecified,
                            fontStyle = FontStyle.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}