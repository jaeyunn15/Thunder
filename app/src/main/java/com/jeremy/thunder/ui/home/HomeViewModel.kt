package com.jeremy.thunder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.socket.model.AllMarketTickerResponseItem
import com.jeremy.thunder.socket.model.BinanceRequest
import com.jeremy.thunder.socket.model.RequestFormatField
import com.jeremy.thunder.socket.model.RequestTicketField
import com.jeremy.thunder.socket.model.RequestTypeField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val service: SocketService
) : ViewModel() {

    private val _allMarketTickerFlow = MutableStateFlow<List<AllMarketTickerResponseItem>>(emptyList())
    val allMarketTickerFlow: Flow<List<AllMarketTickerResponseItem>> get() = _allMarketTickerFlow

    private val _socketEventFlow = MutableStateFlow<String>("")
    val socketEventFlow: Flow<String> get() = _socketEventFlow

    init {
        service.receiveEvent().onEach {
            val state = when (it) {
                is WebSocketEvent.OnConnectionOpen -> {
                    "⚡️ WebSocket Connection Open..."
                }
                is WebSocketEvent.OnMessageReceived -> {
                    "⚡️ WebSocket Receive Message..."
                }
                is WebSocketEvent.OnConnectionError -> {
                    "⚡️ WebSocket Error for ${it.error}..."
                }
                WebSocketEvent.OnConnectionClosed -> {
                    "⚡️ WebSocket Connection Close..."
                }
            }
            _socketEventFlow.update { state }
        }.launchIn(viewModelScope)
    }

    fun requestAllMarketTicker() {
        // upbit socket request
        service.requestUpbit(
            listOf(
                RequestTicketField(ticket = UUID.randomUUID().toString()),
                RequestTypeField(
                    type = "ticker",
                    codes = listOf("KRW-BTC","KRW-ETH")
                ),
                RequestFormatField()
            )
        )

        // binance socket request subscribe
        service.request(
            request = BinanceRequest(
                method = "SUBSCRIBE",
                params = listOf("!ticker@arr"))
        )
    }

    fun requestCancelAllMarketTicker() {
        // binance socket request unsubscribe
        service.request(
            request = BinanceRequest(
                method = "UNSUBSCRIBE",
                params = listOf("!ticker@arr")
            )
        )
    }

    fun observeAllMarket() {
        service.collectUpbitTicker().onEach {
            // this is upbit flow
        }.launchIn(viewModelScope)

        service.observeAllMarketTickers().onEach { response ->
            _allMarketTickerFlow.update { response.data.sortedBy { it.c } }
        }.launchIn(viewModelScope)
    }
}