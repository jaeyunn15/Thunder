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
import com.jeremy.thunder.thunder_internal.event.RequestType
import com.jeremy.thunder.thunder_internal.event.StompSendRequest
import com.jeremy.thunder.thunder_internal.event.StompSubscribeRequest
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_internal.stateDelegate
import com.jeremy.thunder.thunder_state.Active
import com.jeremy.thunder.thunder_state.ConnectState
import com.jeremy.thunder.thunder_state.NetworkState
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Initialize)

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)

    private val headerIdStore by lazy { HeaderIdStore() }

    private val stateCollector: Flow<ConnectState> by stateDelegate(
        connectionListener.collectAppState(),
        networkState.networkStatus,
        _events,
        scope,
        ::recoveryProcess
    )

    override fun getStateOfType(): com.jeremy.thunder.thunder_state.ManagerState {
        return com.jeremy.thunder.thunder_state.StompManager
    }

    override fun collectWebSocketEvent() = _events.asSharedFlow()


    init {
        valveCache.emissionOfValveFlow().map {
            it.map(::requestExecute)
        }.launchIn(innerScope)

        stateCollector.onEach { state ->
            _connectState.update { state }
        }.onStart {
            openConnection {}
        }.launchIn(innerScope)

        _connectState.map(valveCache::onUpdateValve).launchIn(innerScope)
    }

    private fun recoveryProcess() = innerScope.launch {
        connectionRecoveryProcess { requestRecoveryProcess() }
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
            val webSocketRequest = (request as ThunderRequest)
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
        when (message.typeOfRequest) {
            RequestType.STOMP_SUBSCRIBE -> {
                val request = message as StompSubscribeRequest
                if (request.subscribe) {
                    subscribe(
                        topic = request.destination,
                        payload = request.payload.orEmpty()
                    )
                } else {
                    unsubscribe(
                        topic = request.destination
                    )
                }
            }
            RequestType.STOMP_SEND -> {
                val request = message as StompSendRequest
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
        val result = when (message.typeOfRequest) {
            RequestType.STOMP_SEND -> message as StompSendRequest
            RequestType.STOMP_SUBSCRIBE -> message as StompSubscribeRequest
            else -> null
        }
        result?.let {
            valveCache.requestToValve(it)
            recoveryCache.set(it)
        }
    }

    private fun subscribe(topic: String, payload: String) {
        socket?.let { websocket ->
            runCatching {
                val uuid = UUID.randomUUID().toString()
                headerIdStore.put(topic, uuid)
                websocket.send(
                    compileMessage(
                        thunderStompRequest {
                            command = Command.SUBSCRIBE
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