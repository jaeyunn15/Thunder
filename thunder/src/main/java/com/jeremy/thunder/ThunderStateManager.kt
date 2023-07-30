package com.jeremy.thunder

import com.jeremy.thunder.CoroutineScope.scope
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.network.NetworkConnectivityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/*
* Manage ThunderState as SocketState using NetworkState
* ThunderState에 따라 소켓의 전반적인 라이프사이클 관리.
*
* //todo: Lifecycle에 따라 상태 관리 필요, 요청 캐싱 필요
* */

class ThunderStateManager private constructor(
    networkState: NetworkConnectivityService,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
) {
    lateinit var socket: WebSocket

    private val _socketState = MutableStateFlow<ThunderState>(ThunderState.IDLE)
    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    fun collectThunderState() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value

    fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
        networkState.networkStatus.onEach {
            when (it) {
                NetworkState.Available -> {
                    openConnection()
                    _socketState.update { ThunderState.CONNECTING }
                }

                NetworkState.Unavailable -> {
                    closeConnection()
                    _socketState.update { ThunderState.ERROR(ThunderError.NetworkLoss(null)) }
                }
            }
        }.launchIn(scope)
    }

    private fun openConnection() {
        socket = webSocketCore.create()
        socket.events().onEach {
            _events.tryEmit(it)
        }.launchIn(scope)
    }

    private fun closeConnection() {
        if (::socket.isInitialized) {
            socket.close(1000, "shutdown")
        }
    }

    class Factory(
        private val networkStatus: NetworkConnectivityService,
        private val webSocketCore: WebSocket.Factory
    ) {
        fun create(): ThunderStateManager {
            return ThunderStateManager(networkStatus, webSocketCore, scope)
        }
    }
}