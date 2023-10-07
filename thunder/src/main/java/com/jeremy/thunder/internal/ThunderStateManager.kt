package com.jeremy.thunder.internal

import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.cache.RecoveryCache
import com.jeremy.thunder.cache.ValveCache
import com.jeremy.thunder.connection.AppConnectionListener
import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.network.NetworkConnectivityService
import com.jeremy.thunder.state.GetReady
import com.jeremy.thunder.state.Initialize
import com.jeremy.thunder.state.NetworkState
import com.jeremy.thunder.state.ShutDown
import com.jeremy.thunder.state.ThunderError
import com.jeremy.thunder.state.ThunderState
import com.jeremy.thunder.ws.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Manage ThunderState as SocketState using NetworkState
 *
* */

class ThunderStateManager private constructor(
    connectionListener: AppConnectionListener,
    networkState: NetworkConnectivityService,
    private val recoveryCache: RecoveryCache,
    private val valveCache: ValveCache,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
) {
    private var socket: WebSocket? = null

    private val _socketState = MutableStateFlow<ThunderState>(ThunderState.IDLE)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    private var _lastSocketState: ThunderState = ThunderState.IDLE

    /**
     * If the device loses the network or the socket connection fails, it enters the error state below.
     * This is used to use the cache for recovery when a [ThunderState.CONNECTED] is reached.
     * */
    private var isFromError = false

    fun thunderStateAsFlow() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value

    fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
        /**
         * The following code is used to open the valve based on the socket state.
        * */
        _socketState.onEach {
            if (it is ThunderState.ERROR && networkState.hasAvailableNetworks()) {
                closeConnection()
                delay(500)
                openConnection()
            }
            valveCache.onUpdateValveState(it)
        }.launchIn(scope)

        /**
         * When an app is present in a process but offscreen, it automatically controls the connection based on two states to maintain the connection in the meantime.
         * */
        connectionListener.collectState().onEach {
            when(it) {
                Initialize -> Unit
                GetReady -> {
                    _socketState.updateThunderState(ThunderState.CONNECTING)
                    openConnection()
                }
                ShutDown -> closeConnection()
            }
        }.launchIn(scope)

        /**
         * Used to change the ThunderState based on the device's network connection status.
        * */
        networkState.networkStatus.onEach {
            when (it) {
                NetworkState.Available -> {
                    _socketState.updateThunderState(ThunderState.CONNECTING)
                    openConnection()
                }

                NetworkState.Unavailable -> {
                    isFromError = true
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
                    isFromError = true
                    _socketState.updateThunderState(ThunderState.ERROR(ThunderError.SocketLoss(it.error)))
                }
            }
        }.launchIn(scope)

        combine(
            _socketState,
            valveCache.emissionOfValveFlow()
        ) { currentState, request ->
            when (currentState) {
                ThunderState.IDLE -> Unit
                ThunderState.CONNECTING -> {}
                ThunderState.CONNECTED -> {
                    if (isFromError && recoveryCache.hasCache()) {
                        recoveryCache.get()?.let { requestSendMessage(it) }
                        recoveryCache.clear()
                        isFromError = false
                    } else {
                        request.forEach(::requestSendMessage)
                    }
                }
                ThunderState.DISCONNECTING -> {}
                ThunderState.DISCONNECTED -> {}
                is ThunderState.ERROR -> {}
            }
        }.launchIn(scope)
    }

    /**
     * After sending data using the socket, we store it in the recovery cache only after receiving a completion for the event.
    * */
    private fun requestSendMessage(message: String) = socket?.let{
        if (it.send(message)) {
            recoveryCache.set(message)
        }
    }

    private lateinit var connectionJob: Job
    private fun openConnection() {
        if (socket == null) {
            socket = webSocketCore.create()
            socket?.let { webSocket ->
                webSocket.open()
                if (::connectionJob.isInitialized) connectionJob.cancel()
                connectionJob = webSocket.events().onEach { _events.tryEmit(it) }.launchIn(scope)
            }
        }
    }

    private fun closeConnection() {
        socket?.let {
            it.close(1000, "shutdown")
            if (::connectionJob.isInitialized) connectionJob.cancel()
            socket = null
        }
    }

    fun send(key: String, message: String) {
        valveCache.requestToValve(key to message)
    }

    private fun MutableStateFlow<ThunderState>.updateThunderState(state: ThunderState) {
        _lastSocketState = getAndUpdate { state }
    }

    class Factory(
        private val connectionListener: AppConnectionListener,
        private val networkStatus: NetworkConnectivityService,
        private val cacheController: CacheController,
        private val webSocketCore: WebSocket.Factory
    ) {
        fun create(): ThunderStateManager {
            return ThunderStateManager(
                connectionListener = connectionListener,
                networkState = networkStatus,
                recoveryCache = cacheController.rCache,
                valveCache = cacheController.vCache,
                webSocketCore = webSocketCore,
                scope = scope
            )
        }
    }
}