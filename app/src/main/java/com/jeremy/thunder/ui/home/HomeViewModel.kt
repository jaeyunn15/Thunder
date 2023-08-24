package com.jeremy.thunder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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