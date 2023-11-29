package com.jeremy.thunder.internal

import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.thunderLog
import com.jeremy.thunder.thunder_internal.AppConnectionListener
import com.jeremy.thunder.thunder_internal.NetworkConnectivityService
import com.jeremy.thunder.thunder_internal.StateManager
import com.jeremy.thunder.thunder_internal.WebSocket
import com.jeremy.thunder.thunder_internal.cache.BaseRecovery
import com.jeremy.thunder.thunder_internal.cache.BaseValve
import com.jeremy.thunder.thunder_internal.cache.ICacheController
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_internal.event.WebSocketRequest
import com.jeremy.thunder.thunder_state.Active
import com.jeremy.thunder.thunder_state.AppState
import com.jeremy.thunder.thunder_state.ConnectState
import com.jeremy.thunder.thunder_state.Inactive
import com.jeremy.thunder.thunder_state.ManagerState
import com.jeremy.thunder.thunder_state.NetworkState
import com.jeremy.thunder.thunder_state.ThunderManager
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ThunderStateManager private constructor(
    private val connectionListener: AppConnectionListener,
    private val networkState: NetworkConnectivityService,
    private val recoveryCache: BaseRecovery<ThunderRequest>,
    private val valveCache: BaseValve<ThunderRequest>,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
) : StateManager {
    private val innerScope = scope + CoroutineExceptionHandler { _, throwable ->
        thunderLog("[ThunderStateManager] = ${throwable.message}")
    }

    private var socket: WebSocket? = null

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Initialize)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    override fun getStateOfType(): ManagerState = ThunderManager

    override fun collectWebSocketEvent() = _events.asSharedFlow()

    init {
        _connectState.map(valveCache::onUpdateValve).launchIn(innerScope)

        valveCache.emissionOfValveFlow().map { it.map(::requestSendMessage) }.launchIn(innerScope)

        connectionListener.collectAppState() // Application State (Active is Foreground & Background, Inactive is Destroy)
            .mapLatest { appState ->
                appState.checkAndRecovery()
            }.combine(networkState.networkStatus) { upStreamState, networkState -> // Network State (either wifi, mobile)
                if (upStreamState) networkState.checkAndRecovery() else false
            }.combine(_events) { upStreamState, webSocketEvent -> // WebSocket Event flow
                if (upStreamState) {
                    when (webSocketEvent) {
                        is WebSocketEvent.OnMessageReceived,
                        is WebSocketEvent.OnConnectionOpen -> {
                            _connectState.update { ConnectState.Establish }
                        }

                        WebSocketEvent.OnConnectionClosed -> {
                            _connectState.update { ConnectState.ConnectClose() }
                        }

                        is WebSocketEvent.OnConnectionError -> {
                            _connectState.update {
                                ConnectState.ConnectError(
                                    com.jeremy.thunder.thunder_state.ThunderError.SocketLoss(webSocketEvent.error)
                                )
                            }
                            recoveryProcess()
                        }
                    }
                }
            }.onStart {
                openConnection {
                    thunderLog("Open First Connection.")
                }
            }.launchIn(innerScope)
    }

    private suspend fun AppState.checkAndRecovery(): Boolean {
        return when (this) {
            Active -> true
            Inactive -> {
                recoveryProcess()
                false
            }
        }
    }
    private suspend fun NetworkState.checkAndRecovery(): Boolean {
        return when (this) {
            NetworkState.Available -> true
            NetworkState.Unavailable -> {
                recoveryProcess()
                false
            }
        }
    }

    private suspend fun recoveryProcess() {
        connectionRecoveryProcess(
            onConnect = {
                requestRecoveryProcess()
            }
        )
    }

    private suspend fun checkOnValidState(): Boolean = withContext(Dispatchers.Default) {
        val appState = connectionListener.collectAppState().firstOrNull()
        val networkState = networkState.networkStatus.firstOrNull()
        appState == Active && networkState == NetworkState.Available
    }

    private suspend fun connectionRecoveryProcess(onConnect: () -> Unit) {
        if (checkOnValidState()) {
            retryConnection(
                onConnect = onConnect,
            )
        }
    }

    private fun requestRecoveryProcess() {
        if (recoveryCache.hasCache()) {
            val request = recoveryCache.get()
            val webSocketRequest = (request as WebSocketRequest)
            valveCache.requestToValve(webSocketRequest)
            recoveryCache.clear()
        }
    }

    override fun retryConnection(onConnect: () -> Unit) {
        thunderLog("Thunder retry connection work.")
        closeConnection {
            openConnection(onConnect)
        }
    }

    /**
     * After sending data using the socket, we store it in the recovery cache only after receiving a completion for the event.
     * */
    private fun requestSendMessage(message: ThunderRequest) = socket?.let {
        val msg = (message as WebSocketRequest).msg
        if (it.send(msg)) {
            recoveryCache.set(message)
        }
    }

    override fun openConnection(onConnect: () -> Unit) = synchronized(this) {
        if (socket == null) {
            socket = webSocketCore.create()
            socket?.let { webSocket ->
                webSocket.open()
                    .onStart {
                        onConnect.invoke()
                        thunderLog("Thunder open connection work start.")
                    }
                    .onEach { _events.tryEmit(it) }
                    .launchIn(innerScope)
            }
        }
    }

    override fun closeConnection(onDisconnect: () -> Unit): Unit = synchronized(this) {
        socket?.let {
            if (it.close(1000, "shutdown")) {
                thunderLog("Thunder close connection work success.")
            } else {
                thunderLog("Thunder close connection work failed because websocket already shutdown.")
            }
            socket = null
            onDisconnect.invoke()
        }
    }

    override fun send(message: ThunderRequest) {
        valveCache.requestToValve(message)
    }

    class Factory : StateManager.Factory {
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
        private const val RETRY_CONNECTION_GAP = 500L
    }
}