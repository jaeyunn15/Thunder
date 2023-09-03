package com.jeremy.thunder.internal

import com.jeremy.thunder.CoroutineScope.scope
import com.jeremy.thunder.NetworkState
import com.jeremy.thunder.ThunderError
import com.jeremy.thunder.ThunderState
import com.jeremy.thunder.WebSocket
import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.cache.RecoveryCache
import com.jeremy.thunder.cache.ValveCache
import com.jeremy.thunder.event.WebSocketEvent
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
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
) {
    lateinit var socket: WebSocket

    private val _socketState = MutableStateFlow<ThunderState>(ThunderState.IDLE)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    private var _lastSocketState: ThunderState = ThunderState.IDLE

    fun thunderStateAsFlow() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value

    fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
        _socketState.onEach {
            valveCache.onUpdateValveState(it)
        }.launchIn(scope)

        networkState.networkStatus.onEach {
            when (it) {
                NetworkState.Available -> {
                    _socketState.updateThunderState(ThunderState.CONNECTING)
                    openConnection()
                }

                NetworkState.Unavailable -> {
                    _socketState.updateThunderState(ThunderState.ERROR(ThunderError.NetworkLoss(null)))
                    closeConnection()
                }
            }
        }.launchIn(scope)

        _events.onEach {
            when (it) {
                is WebSocketEvent.OnConnectionOpen -> {
                    _socketState.updateThunderState(ThunderState.CONNECTED)
                }

                is WebSocketEvent.OnMessageReceived -> Unit

                WebSocketEvent.OnConnectionClosed -> {
                    _socketState.updateThunderState(ThunderState.DISCONNECTED)
                }

                is WebSocketEvent.OnConnectionError -> {
                    _socketState.updateThunderState(ThunderState.ERROR(ThunderError.SocketLoss(it.error)))
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
                ThunderState.IDLE -> Unit
                ThunderState.CONNECTING -> {
                }
                ThunderState.CONNECTED -> {
                    if (_lastSocketState is ThunderState.ERROR && recoveryCache.hasCache()) {
                        // Last State : ERROR - this is recovery for socket, network error
                        recoveryCache.get()?.let {
                            requestSendMessage(it)
                        }
                        recoveryCache.clear()
                    } else {
                        // General State
                        request.forEach(::requestSendMessage)
                    }
                }
                ThunderState.DISCONNECTING -> {

                }
                ThunderState.DISCONNECTED -> {

                }
                is ThunderState.ERROR -> {

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

    private fun MutableStateFlow<ThunderState>.updateThunderState(state: ThunderState) {
        _lastSocketState = getAndUpdate { state }
    }

    class Factory(
        private val networkStatus: NetworkConnectivityService,
        private val cacheController: CacheController,
        private val webSocketCore: WebSocket.Factory
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