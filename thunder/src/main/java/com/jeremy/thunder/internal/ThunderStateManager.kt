package com.jeremy.thunder.internal

import com.jeremy.thunder.CoroutineScope.scope
import com.jeremy.thunder.NetworkState
import com.jeremy.thunder.ThunderError
import com.jeremy.thunder.ThunderState
import com.jeremy.thunder.WebSocket
import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.network.NetworkConnectivityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
* Manage ThunderState as SocketState using NetworkState
* ThunderState에 따라 Socket Lifecycle Manage
*
* //23.08.15: 캐싱 처리 정책 필요 (시작-종단 지점이 불명확)
* */

class ThunderStateManager private constructor(
    networkState: NetworkConnectivityService,
    private val cacheController: CacheController,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
) {
    lateinit var socket: WebSocket

    private val _requestFlow = MutableSharedFlow<Pair<String, String>?>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val _socketState = MutableStateFlow<ThunderState>(ThunderState.IDLE)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    private var _lastSocketState: ThunderState = ThunderState.IDLE

    fun collectThunderState() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value

    fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
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

        combine(
            _socketState,
            _requestFlow.filterNotNull()
        ) { currentState, request ->
            when (currentState) {
                ThunderState.IDLE -> Unit
                ThunderState.CONNECTING -> {
                    cacheController.set(request.first, request.second)
                }
                ThunderState.CONNECTED -> {
                    if (
                        (_lastSocketState is ThunderState.ERROR || _lastSocketState is ThunderState.CONNECTING)
                        && cacheController.hasCache()
                    ) {
                        // Last State : CONNECTING, ERROR
                        cacheController.get().forEach(::requestSendMessage)
                        cacheController.clear()
                    } else {
                        // General State
                        requestSendMessage(request.second)
                    }
                }
                ThunderState.DISCONNECTING -> {
                    cacheController.clear()
                }
                ThunderState.DISCONNECTED -> {
                    cacheController.clear()
                }
                is ThunderState.ERROR -> {
                    cacheController.set(request.first, request.second)
                }
            }
        }.launchIn(scope)
    }

    private fun requestSendMessage(message: String) {
        socket.send(message)
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
        _requestFlow.tryEmit(key to message)
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
                cacheController = cacheController,
                webSocketCore = webSocketCore,
                scope = scope
            )
        }
    }
}