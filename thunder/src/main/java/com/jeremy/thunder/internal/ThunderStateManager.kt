package com.jeremy.thunder.internal

import com.jeremy.thunder.CoroutineScope.scope
import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.cache.RecoveryCache
import com.jeremy.thunder.cache.ValveCache
import com.jeremy.thunder.network.NetworkConnectivityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
* Manage ThunderState as SocketState using NetworkState
* ThunderState에 따라 Socket Lifecycle Manage
*
* */

class ThunderStateManager private constructor(
    networkState: NetworkConnectivityService,
    private val recoveryCache: RecoveryCache,
    private val valveCache: ValveCache,
    private val webSocketCore: com.jeremy.thunder.core.WebSocket.Factory,
    private val scope: CoroutineScope
) {
    lateinit var socket: com.jeremy.thunder.core.WebSocket

    private val _socketState = MutableStateFlow<com.jeremy.thunder.core.ThunderState>(com.jeremy.thunder.core.ThunderState.IDLE)

    private val _events = MutableSharedFlow<com.jeremy.thunder.core.WebSocketEvent>(replay = 1)

    private var _lastSocketState: com.jeremy.thunder.core.ThunderState = com.jeremy.thunder.core.ThunderState.IDLE

    fun thunderStateAsFlow() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value

    fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
        _socketState.onEach {
            valveCache.onUpdateValveState(it)
        }.launchIn(scope)

        networkState.networkStatus.onEach {
            when (it) {
                com.jeremy.thunder.core.NetworkState.Available -> {
                    _socketState.updateThunderState(com.jeremy.thunder.core.ThunderState.CONNECTING)
                    openConnection()
                }

                com.jeremy.thunder.core.NetworkState.Unavailable -> {
                    _socketState.updateThunderState(com.jeremy.thunder.core.ThunderState.ERROR(com.jeremy.thunder.core.ThunderError.NetworkLoss(null)))
                    closeConnection()
                }
            }
        }.launchIn(scope)

        _events.onEach {
            when (it) {
                is com.jeremy.thunder.core.WebSocketEvent.OnConnectionOpen -> {
                    _socketState.updateThunderState(com.jeremy.thunder.core.ThunderState.CONNECTED)
                }

                is com.jeremy.thunder.core.WebSocketEvent.OnMessageReceived -> Unit

                com.jeremy.thunder.core.WebSocketEvent.OnConnectionClosed -> {
                    _socketState.updateThunderState(com.jeremy.thunder.core.ThunderState.DISCONNECTED)
                }

                is com.jeremy.thunder.core.WebSocketEvent.OnConnectionError -> {
                    _socketState.updateThunderState(com.jeremy.thunder.core.ThunderState.ERROR(com.jeremy.thunder.core.ThunderError.SocketLoss(it.error)))
                }
            }
        }.launchIn(scope)

        /*
        * requestFlow로 요청 사항이 흘러 들어오면 우선적으로 ValveControl에 보냄
        * valve 활성화에 따라 다시 흘려보냄.
        * */

        combine(
            _socketState,
            valveCache.emissionOfValveFlow()
        ) { currentState, request ->

            when (currentState) {
                com.jeremy.thunder.core.ThunderState.IDLE -> Unit
                com.jeremy.thunder.core.ThunderState.CONNECTING -> {
                }
                com.jeremy.thunder.core.ThunderState.CONNECTED -> {
                    if (_lastSocketState is com.jeremy.thunder.core.ThunderState.ERROR && recoveryCache.hasCache()) {
                        // Last State : ERROR - this is recovery for socket, network error
                        recoveryCache.get().forEach(::requestSendMessage)
                        recoveryCache.clear()
                    } else {
                        // General State
                        request.forEach(::requestSendMessage)
                    }
                }
                com.jeremy.thunder.core.ThunderState.DISCONNECTING -> {

                }
                com.jeremy.thunder.core.ThunderState.DISCONNECTED -> {

                }
                is com.jeremy.thunder.core.ThunderState.ERROR -> {

                }
            }
        }.launchIn(scope)
    }

    private fun requestSendMessage(message: String) {
        if (socket.send(message)) {
            recoveryCache.set(message)
        }
    }

    private lateinit var connectionJob: Job

    private fun openConnection() {
        socket = webSocketCore.create()
        socket.open()
        if (::connectionJob.isInitialized) connectionJob.cancel()
        connectionJob = socket.events().onEach { _events.tryEmit(it) }.launchIn(scope)
    }

    private fun closeConnection() {
        if (::socket.isInitialized) {
            socket.close(1000, "shutdown")
            if (::connectionJob.isInitialized) connectionJob.cancel()
        }
    }

    fun send(key: String, message: String) {
        valveCache.requestToValve(key to message)
    }

    private fun MutableStateFlow<com.jeremy.thunder.core.ThunderState>.updateThunderState(state: com.jeremy.thunder.core.ThunderState) {
        _lastSocketState = getAndUpdate { state }
    }

    class Factory(
        private val networkStatus: NetworkConnectivityService,
        private val cacheController: CacheController,
        private val webSocketCore: com.jeremy.thunder.core.WebSocket.Factory
    ) {
        fun create(): ThunderStateManager {
            return ThunderStateManager(
                networkState = networkStatus,
                recoveryCache = cacheController.rCache,
                valveCache = cacheController.vCache,
                webSocketCore = webSocketCore,
                scope = scope
            )
        }
    }
}