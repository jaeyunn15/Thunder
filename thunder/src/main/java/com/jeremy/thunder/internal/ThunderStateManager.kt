package com.jeremy.thunder.internal

import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.thunderLog
import com.jeremy.thunder.thunder_internal.AppConnectionListener
import com.jeremy.thunder.thunder_internal.BaseRecovery
import com.jeremy.thunder.thunder_internal.BaseValve
import com.jeremy.thunder.thunder_internal.ICacheController
import com.jeremy.thunder.thunder_internal.NetworkConnectivityService
import com.jeremy.thunder.thunder_internal.StateManager
import com.jeremy.thunder.thunder_internal.WebSocket
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import com.jeremy.thunder.thunder_internal.event.WebSocketRequest
import com.jeremy.thunder.thunder_internal.state.Background
import com.jeremy.thunder.thunder_internal.state.Foreground
import com.jeremy.thunder.thunder_internal.state.Initialize
import com.jeremy.thunder.thunder_internal.state.ManagerState
import com.jeremy.thunder.thunder_internal.state.NetworkState
import com.jeremy.thunder.thunder_internal.state.ShutDown
import com.jeremy.thunder.thunder_internal.state.ThunderError
import com.jeremy.thunder.thunder_internal.state.ThunderManager
import com.jeremy.thunder.thunder_internal.state.ThunderState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus

/**
 * Manage ThunderState as SocketState using NetworkState
 *
* */

class ThunderStateManager private constructor(
    connectionListener: AppConnectionListener,
    networkState: NetworkConnectivityService,
    private val recoveryCache: BaseRecovery<ThunderRequest>,
    private val valveCache: BaseValve<ThunderRequest>,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
): StateManager {
    private val innerScope = scope + CoroutineExceptionHandler { _, throwable ->
        thunderLog("[ThunderStateManager] = ${throwable.message}")
    }

    private var socket: WebSocket? = null

    private val _socketState = MutableStateFlow<ThunderState>(ThunderState.IDLE)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    /**
     * If the device loses the network or the socket connection fails, it enters the error state below.
     * This is used to use the cache for recovery when a [ThunderState.CONNECTED] is reached.
     * */
    private var isReSubscription = false

    private val _retryNeedFlag = MutableStateFlow<Boolean>(false)

    fun thunderStateAsFlow() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value
    override fun getStateOfType(): ManagerState {
        return ThunderManager
    }

    override fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
        /**
         * Update SocketState as ThunderState.
         * */
        _events.onEach { event ->
            when (event) {
                is WebSocketEvent.OnConnectionOpen -> {
                    _socketState.update { ThunderState.CONNECTED }
                }

                is WebSocketEvent.OnMessageReceived -> Unit

                WebSocketEvent.OnConnectionClosed -> {
                    _socketState.update { ThunderState.DISCONNECTED }
                }

                is WebSocketEvent.OnConnectionError -> {
                    isReSubscription = true
                    _socketState.update { ThunderState.ERROR(ThunderError.SocketLoss(event.error)) }
                }
            }
        }.launchIn(innerScope)

        /**
         * Open, Retry Connection work as network state.
         * */
        combine(
            _retryNeedFlag,
            networkState.networkStatus
        ) { retry, network ->
            when (network) {
                NetworkState.Available -> {
                    if (retry) {
                        retryConnection()
                    } else {
                        openConnection()
                    }
                }
                NetworkState.Unavailable -> {
                    _socketState.update { ThunderState.ERROR(ThunderError.NetworkLoss) }
                }
            }
        }.launchIn(innerScope)

        /**
         * Update RetryFlag And Valve And request socket message as upstream state.
         * */
        combine(
            _socketState,
            connectionListener.collectState()
        ) { socketState, appState ->
            when (appState) {
                Initialize -> {
                    openConnection()
                }
                Foreground -> {
                    if (socketState is ThunderState.ERROR) {
                        _retryNeedFlag.update { true }
                    }
                }
                Background -> {}
                ShutDown -> {
                    closeConnection()
                }
            }
            valveCache.onUpdateValveState(socketState)
        }.launchIn(innerScope)

        combine(
            _socketState,
            valveCache.emissionOfValveFlow()
        ) { currentSocketState, request ->
            when (currentSocketState) {
                ThunderState.CONNECTED -> {
                    if (isReSubscription && recoveryCache.hasCache()) {
                        recoveryCache.get()?.let { requestSendMessage(it) }
                        recoveryCache.clear()
                        isReSubscription = false
                    } else {
                        request.forEach(::requestSendMessage)
                    }
                }

                else -> Unit
            }
        }.launchIn(innerScope)
    }

    private suspend fun retryConnection() {
        thunderLog("Thunder retry connection work.")
        closeConnection()
        delay(RETRY_CONNECTION_GAP)
        openConnection()
        _retryNeedFlag.update { false }
    }

    /**
     * After sending data using the socket, we store it in the recovery cache only after receiving a completion for the event.
    * */
    private fun requestSendMessage(message: ThunderRequest) = socket?.let{
        val msg = (message as WebSocketRequest).msg
        if (it.send(msg)) {
            recoveryCache.set(message)
        }
    }

    private lateinit var connectionJob: Job
    private fun openConnection() = synchronized(this) {
        if (socket == null) {
            socket = webSocketCore.create()
            socket?.let { webSocket ->
                if (::connectionJob.isInitialized) connectionJob.cancel()
                connectionJob = webSocket.open().onEach { _events.tryEmit(it) }.launchIn(innerScope)
                thunderLog("Thunder open connection work.")
            }
        }
    }

    private fun closeConnection() = synchronized(this) {
        socket?.let {
            thunderLog("Thunder close connection work.")
            _socketState.update { ThunderState.ERROR() }
            if (it.close(1000, "shutdown")) socket = null
            if (::connectionJob.isInitialized) connectionJob.cancel()
        }
    }

    override fun send(message: ThunderRequest) {
        valveCache.requestToValve(message)
    }

    class Factory: StateManager.Factory {
        override fun create(
            connectionListener: AppConnectionListener,
            networkStatus: NetworkConnectivityService,
            cacheController: ICacheController<ThunderRequest>,
            webSocketCore: WebSocket.Factory
        ): StateManager {
            return ThunderStateManager(
                connectionListener = connectionListener,
                networkState = networkStatus,
                recoveryCache = cacheController.getRecovery(),
                valveCache = cacheController.getValve(),
                webSocketCore = webSocketCore,
                scope = scope
            )
        }
    }

    companion object {
        private const val RETRY_CONNECTION_GAP = 1_000L
    }
}