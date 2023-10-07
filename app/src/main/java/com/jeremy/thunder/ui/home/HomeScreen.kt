package com.jeremy.thunder.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.thunder.OnLifecycleEvent
import com.jeremy.thunder.socket.model.AllMarketTickerResponseItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(keys = emptyArray(), block = {
        viewModel.observeAllMarket()
    })

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.requestAllMarketTicker()
        }
        if (event == Lifecycle.Event.ON_PAUSE) {
            viewModel.requestCancelAllMarketTicker()
        }
    }

    val tickerFlow =
        viewModel.allMarketTickerFlow.collectAsStateWithLifecycle(initialValue = emptyList()).value

    val socketEventFlow =
        viewModel.socketEventFlow.collectAsStateWithLifecycle(initialValue = "").value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 15.dp, bottom = 15.dp, end = 10.dp, start = 10.dp),
    ) {
        Text(
            text = "[WebSocket State] >> \n$socketEventFlow",
            color = Color.Gray,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(content = {
            items(
                count = tickerFlow.size,
                key = { tickerFlow[it].s },
                itemContent = {
                    CryptoPriceItem(tickerFlow[it])
                })
        })
    }
}

@Composable
fun CryptoPriceItem(
    item: AllMarketTickerResponseItem
) {
    Row(modifier = Modifier.padding(top = 15.dp, bottom = 15.dp, end = 10.dp, start = 10.dp)) {
        Text(text = item.s, fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = item.c)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = item.P + "%",
            modifier = Modifier.widthIn(30.dp),
            fontWeight = FontWeight.Bold,
            color = if (item.P.contains("-")) Color.Blue else Color.Red
        )
    }
}