package com.jeremy.thunder.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.socket.model.BinanceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val service: SocketService
) : ViewModel() {

    fun requestSpecificTicker() {
        viewModelScope.launch {
            // 특정 2개의 티커만 조회
            service.request(
                request = BinanceRequest(
                    method = "SUBSCRIBE",
                    params = listOf(
                        "btcusdt@markPrice",
                        "ethusdt@markPrice"
                    )
                )
            )
        }
    }

    fun requestCancelSpecificTicker() {
        service.request(
            request = BinanceRequest(
                method = "UNSUBSCRIBE",
                params = listOf(
                    "btcusdt@markPrice",
                    "ethusdt@markPrice"
                )
            )
        )
    }

    fun observeSpecificTicker() {
        service.observeTicker().onEach {

        }.launchIn(viewModelScope)
    }
}