package com.jeremy.thunder

import com.jeremy.thunder.network.NetworkConnectivityService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ThunderConnection private constructor(
    networkState: NetworkConnectivityService,
    private val webSocketCore: WebSocket.Factory
) {
    lateinit var socket: WebSocket

    private val _socketState = MutableStateFlow<ThunderState>(ThunderState.IDLE)

    fun collectThunderState() = _socketState.asStateFlow()

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
        }.launchIn(GlobalScope)
    }

    private fun openConnection() {
        socket = webSocketCore.create()
    }

    private fun closeConnection() {
        if (::socket.isInitialized) {
            socket.cancel()
        }
    }


    class Factory(
        private val networkStatus: NetworkConnectivityService,
        private val webSocketCore: WebSocket.Factory
    ) {
        fun create(): ThunderConnection {
            return ThunderConnection(networkStatus, webSocketCore)
        }
    }
}