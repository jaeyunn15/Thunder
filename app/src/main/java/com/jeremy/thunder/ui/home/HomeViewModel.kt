package com.jeremy.thunder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.socket.model.AllMarketTickerResponseItem
import com.jeremy.thunder.socket.model.BinanceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
        service.request(
            request = BinanceRequest(
                method = "SUBSCRIBE",
                params = listOf("!ticker@arr"))
        )
    }

    fun requestCancelAllMarketTicker() {
        service.request(
            request = BinanceRequest(
                method = "UNSUBSCRIBE",
                params = listOf("!ticker@arr")
            )
        )
    }

    fun observeAllMarket() {
        service.observeAllMarketTickers().onEach { response ->
            _allMarketTickerFlow.update { response.data.sortedBy { it.c } }
        }.launchIn(viewModelScope)
    }
}