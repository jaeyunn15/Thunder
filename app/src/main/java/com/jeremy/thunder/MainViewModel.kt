package com.jeremy.thunder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.thunder.socket.BinanceRequest
import com.jeremy.thunder.socket.SocketService
import com.jeremy.thunder.socket.Ticker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val service: SocketService
): ViewModel() {

    private val _response = MutableStateFlow<Ticker?>(null)
    val response: StateFlow<Ticker?> = _response.asStateFlow()

    fun request() {
        viewModelScope.launch {
            delay(4000) // thunder state observer가 생성 되기 이전 임으로 임의 딜레이
            service.request(request = BinanceRequest())
        }
    }

    fun observeResponse() {
        service.observeTicker().onEach { result ->
            _response.update { result.data }
        }.launchIn(viewModelScope)
    }
}