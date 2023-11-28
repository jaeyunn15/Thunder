package com.jeremy.thunder.stomp.internal

import com.jeremy.thunder.stomp.compiler.MessageCompiler.compileMessage
import com.jeremy.thunder.stomp.compiler.thunderStompRequest
import com.jeremy.thunder.stomp.model.ACK
import com.jeremy.thunder.stomp.model.Command
import com.jeremy.thunder.stomp.model.DEFAULT_ACK
import com.jeremy.thunder.stomp.model.DESTINATION
import com.jeremy.thunder.stomp.model.ID
import com.jeremy.thunder.stomp.model.SUPPORTED_VERSIONS
import com.jeremy.thunder.stomp.model.VERSION
import com.jeremy.thunder.thunder_internal.AppConnectionListener
import com.jeremy.thunder.thunder_internal.NetworkConnectivityService
import com.jeremy.thunder.thunder_internal.StateManager
import com.jeremy.thunder.thunder_internal.WebSocket
import com.jeremy.thunder.thunder_internal.cache.BaseRecovery
import com.jeremy.thunder.thunder_internal.cache.BaseValve
import com.jeremy.thunder.thunder_internal.cache.ICacheController
import com.jeremy.thunder.thunder_internal.event.StompRequest
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_internal.event.WebSocketRequest
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.util.UUID

class StompStateManager private constructor(
    private val connectionListener: AppConnectionListener,
    private val networkState: NetworkConnectivityService,
    private val recoveryCache: BaseRecovery<ThunderRequest>,
    private val valveCache: BaseValve<ThunderRequest>,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
): StateManager {
    private val innerScope = scope + CoroutineExceptionHandler { _, throwable ->

    }

    private var socket: WebSocket? = null

    private val _connectState = MutableStateFlow<com.jeremy.thunder.thunder_state.ConnectState>(com.jeremy.thunder.thunder_state.ConnectState.Initialize)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    private val headerIdStore by lazy { HeaderIdStore() }

    override fun getStateOfType(): com.jeremy.thunder.thunder_state.ManagerState {
        return com.jeremy.thunder.thunder_state.StompManager
    }

    override fun collectWebSocketEvent() = _events.asSharedFlow()


    init {
        _connectState.map(valveCache::onUpdateValve).launchIn(innerScope)

        connectionListener.collectAppState()
            .mapLatest { appState ->
                when (appState) {
                    com.jeremy.thunder.thunder_state.Inactive -> {
                        recoveryProcess()
                        false
                    }

                    com.jeremy.thunder.thunder_state.Active -> {
                        true
                    }
                }
            }.combine(networkState.networkStatus) { upStreamState, networkState ->
                if (upStreamState) {
                    when (networkState) {
                        com.jeremy.thunder.thunder_state.NetworkState.Unavailable -> {
                            recoveryProcess()
                            false
                        }

                        com.jeremy.thunder.thunder_state.NetworkState.Available -> {
                            true
                        }
                    }
                } else false
            }.combine(_events) { upStreamState, webSocketEvent ->
                if (upStreamState) {
                    when (webSocketEvent) {
                        is WebSocketEvent.OnMessageReceived -> Unit
                        is WebSocketEvent.OnConnectionOpen -> {
                            _connectState.update { com.jeremy.thunder.thunder_state.ConnectState.Establish }
                        }

                        WebSocketEvent.OnConnectionClosed -> {
                            _connectState.update { com.jeremy.thunder.thunder_state.ConnectState.ConnectClose() }
                        }

                        is WebSocketEvent.OnConnectionError -> {
                            _connectState.update {
                                com.jeremy.thunder.thunder_state.ConnectState.ConnectError(
                                    com.jeremy.thunder.thunder_state.ThunderError.SocketLoss(webSocketEvent.error)
                                )
                            }
                            recoveryProcess()
                        }
                    }
                }
            }.onStart {
                openConnection {}
            }.launchIn(innerScope)

        combine(
            _connectState,
            valveCache.emissionOfValveFlow(),
        ) { connectState, valve ->
            if (connectState is com.jeremy.thunder.thunder_state.ConnectState.Establish) {
                valve.forEach(::requestExecute)
            }
        }.launchIn(innerScope)
    }

    private suspend fun recoveryProcess() {
        connectionRecoveryProcess(
            onConnect = {
                requestRecoveryProcess()
            },
        )
    }

    private suspend fun checkOnValidState(): Boolean = withContext(Dispatchers.Default) {
        val appState = connectionListener.collectAppState().firstOrNull()
        val networkState = networkState.networkStatus.firstOrNull()
        appState == com.jeremy.thunder.thunder_state.Active && networkState == com.jeremy.thunder.thunder_state.NetworkState.Available
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
        closeConnection {
            openConnection(onConnect)
        }
    }

    private fun requestExecute(message: ThunderRequest) = innerScope.launch(Dispatchers.IO) {
        val request = message as StompRequest
        when (request.command) {
            Command.SUBSCRIBE.name.lowercase() -> {
                subscribe(
                    topic = request.destination,
                    payload = request.payload.orEmpty()
                )
            }
            Command.UNSUBSCRIBE.name.lowercase() -> {
                unsubscribe(
                    topic = request.destination
                )
            }
            Command.SEND.name.lowercase() -> {
                sendMessage(
                    topic = request.destination,
                    payload = request.payload.orEmpty()
                )
            }
            else -> {
                // not implement yet
            }
        }
    }

    /**
     * After sending data using the socket, we store it in the recovery cache only after receiving a completion for the event.
     * */
    private fun sendMessage(topic: String, payload: String) {
        socket?.let {
            runCatching {
                val uuid = UUID.randomUUID().toString()
                headerIdStore.put(topic, uuid)
                it.send(
                    compileMessage(
                        thunderStompRequest {
                            this.command = Command.SEND
                            header {
                                ID to uuid
                                DESTINATION to topic
                                ACK to DEFAULT_ACK
                            }
                            this.payload = payload
                        }
                    )
                )
            }
        }
    }

    override fun openConnection(onConnect: () -> Unit) = synchronized(this) {
        if (socket == null) {
            socket = webSocketCore.create()
            socket?.let { webSocket ->
                webSocket.open().onEach {
                    if (it is WebSocketEvent.OnConnectionOpen) {
                        onConnect.invoke()
                        webSocket.send(
                            compileMessage(
                                thunderStompRequest {
                                    command = Command.CONNECT
                                    header {
                                        VERSION to SUPPORTED_VERSIONS
                                    }
                                }
                            )
                        )
                    }
                    _events.tryEmit(it)
                }.launchIn(innerScope)
            }
        }
    }

    override fun closeConnection(onDisconnect: () -> Unit): Unit = synchronized(this) {
        socket?.let { websocket ->
            websocket.close(1000, "shutdown")
            socket = null
            headerIdStore.clear()
            onDisconnect.invoke()
        }
    }

    override fun send(message: ThunderRequest) {
        val request = message as StompRequest
        valveCache.requestToValve(request)
        recoveryCache.set(request)
    }

    private fun subscribe(topic: String, payload: String) {
        socket?.let { websocket ->
            runCatching {
                val uuid = UUID.randomUUID().toString()
                headerIdStore.put(topic, uuid)
                val request = compileMessage(thunderStompRequest {
                    this.command = Command.SUBSCRIBE
                    header {
                        ID to uuid
                        DESTINATION to topic
                        ACK to DEFAULT_ACK
                    }
                    this.payload = payload
                })
                websocket.send(
                    request
                )
            }
        }
    }

    private  fun unsubscribe(topic: String) {
        socket?.let { websocket ->
            runCatching {
                websocket.send(
                    compileMessage(
                        thunderStompRequest {
                            this.command = Command.UNSUBSCRIBE
                            header {
                                ID to headerIdStore[topic]
                                DESTINATION to topic
                                ACK to DEFAULT_ACK
                            }
                        }
                    )
                )
            }
        }
    }

    class Factory: StateManager.Factory {
        override fun create(
            connectionListener: AppConnectionListener,
            networkStatus: NetworkConnectivityService,
            cacheController: ICacheController<ThunderRequest>,
            webSocketCore: WebSocket.Factory
        ): StateManager {
            return StompStateManager(
                connectionListener = connectionListener,
                networkState = networkStatus,
                recoveryCache = cacheController.getRecovery(),
                valveCache = cacheController.getValve(),
                webSocketCore = webSocketCore,
                scope = CoroutineScope(SupervisorJob())
            )
        }
    }

    companion object {
        private const val RETRY_CONNECTION_GAP = 1_000L
    }
}