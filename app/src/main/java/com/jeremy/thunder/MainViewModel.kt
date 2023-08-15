package com.jeremy.thunder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.thunder.socket.BinanceRequest
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.socket.Ticker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val service: SocketService
) : ViewModel() {

    private val _response = MutableStateFlow<Ticker?>(null)
    val response: StateFlow<Ticker?> = _response.asStateFlow()

    fun request() {
        viewModelScope.launch {
            // 특정 2개의 티커만 조회
            service.request(
                request = BinanceRequest(
                    params = listOf(
                        "btcusdt@markPrice",
                        "ethusdt@markPrice"
                    )
                )
            )
        }
    }

    fun requestAllMarket() {
        //전체 마켓 데이터 조회
        service.request(request = BinanceRequest(params = listOf("!markPrice@arr")))
    }

    fun observeAllMarket() {
        service.observeAllMarkets().onEach {

        }.launchIn(viewModelScope)
    }

    fun observeTicker() {
        service.observeTicker().onEach {

        }.launchIn(viewModelScope)
    }
}