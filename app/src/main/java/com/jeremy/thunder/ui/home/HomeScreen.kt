package com.jeremy.thunder.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.thunder.OnLifecycleEvent
import com.jeremy.thunder.format
import com.jeremy.thunder.socket.model.AllMarketTickerResponseItem

@RequiresApi(Build.VERSION_CODES.O)
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CryptoPriceItem(
    item: AllMarketTickerResponseItem
) {
    Row(
        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, end = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.s,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = item.P + "%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.P.contains("-")) Color.Blue else Color.Red
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$" + item.c,
                fontSize = 14.sp,
            )
        }
    }
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Color.Gray))
}