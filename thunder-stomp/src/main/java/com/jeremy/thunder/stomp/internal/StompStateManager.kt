package com.jeremy.thunder.stomp.internal

import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.cache.RecoveryCache
import com.jeremy.thunder.cache.ValveCache
import com.jeremy.thunder.connection.AppConnectionListener
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.internal.StateManager
import com.jeremy.thunder.network.NetworkConnectivityService
import com.jeremy.thunder.state.Background
import com.jeremy.thunder.state.Foreground
import com.jeremy.thunder.state.Initialize
import com.jeremy.thunder.state.ManagerState
import com.jeremy.thunder.state.NetworkState
import com.jeremy.thunder.state.ShutDown
import com.jeremy.thunder.state.StompManager
import com.jeremy.thunder.state.StompRequest
import com.jeremy.thunder.state.ThunderError
import com.jeremy.thunder.state.ThunderState
import com.jeremy.thunder.stomp.compiler.MessageCompiler.compileMessage
import com.jeremy.thunder.stomp.compiler.ThunderRequest
import com.jeremy.thunder.stomp.model.ACK
import com.jeremy.thunder.stomp.model.Command
import com.jeremy.thunder.stomp.model.DEFAULT_ACK
import com.jeremy.thunder.stomp.model.DESTINATION
import com.jeremy.thunder.stomp.model.ID
import com.jeremy.thunder.stomp.model.SUPPORTED_VERSIONS
import com.jeremy.thunder.stomp.model.VERSION
import com.jeremy.thunder.ws.WebSocket
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

class StompStateManager private constructor(
    connectionListener: AppConnectionListener,
    networkState: NetworkConnectivityService,
    private val recoveryCache: RecoveryCache,
    private val valveCache: ValveCache,
    private val webSocketCore: WebSocket.Factory,
    private val scope: CoroutineScope
): StateManager {
    private val innerScope = scope + CoroutineExceptionHandler { _, throwable ->
        //thunderLog("[ThunderStateManager] = ${throwable.message}")
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

    private val _socketHeaderUUID: HashMap<String, Queue<String>> = hashMapOf()

    fun thunderStateAsFlow() = _socketState.asStateFlow()

    fun thunderState() = _socketState.value
    override fun getStateOfType(): ManagerState {
        return StompManager
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
            valveCache.
            emissionOfValveFlow()
        ) { currentSocketState, request ->
            when (currentSocketState) {
                ThunderState.CONNECTED -> {
                    if (isReSubscription && recoveryCache.hasCache()) {
                        recoveryCache.get()?.let { requestExecute(it) }
                        recoveryCache.clear()
                        isReSubscription = false
                    } else {
                        request.forEach(::requestExecute)
                    }
                }

                else -> Unit
            }
        }.launchIn(innerScope)
    }

    private suspend fun retryConnection() {
        closeConnection()
        delay(RETRY_CONNECTION_GAP)
        openConnection()
        _retryNeedFlag.update { false }
    }

    private fun requestExecute(message: com.jeremy.thunder.state.ThunderRequest) = innerScope.launch(Dispatchers.IO) {
        val request = message as StompRequest
        when (request.command) {
            "subscribe" -> {
                subscribe(
                    topic = request.destination,
                    payload = request.payload.orEmpty()
                )
            }
            "unsubscribe" -> {
                unsubscribe(
                    topic = request.destination
                )
            }
            "send" -> {
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
                manageHeaderUuid(topic, uuid)
                it.send(
                    compileMessage(
                        ThunderRequest.Builder()
                            .command(Command.SEND)
                            .header(
                                ID to uuid,
                                DESTINATION to topic,
                                ACK to DEFAULT_ACK,
                            )
                            .payload(payload)
                            .build()
                    )
                )
            }
        }
    }

    private lateinit var connectionJob: Job

    private fun openConnection() = synchronized(this) {
        if (socket == null) {
            socket = webSocketCore.create()
            socket?.let { webSocket ->
                if (::connectionJob.isInitialized) connectionJob.cancel()
                connectionJob = webSocket.open().onEach {
                    if (it is WebSocketEvent.OnConnectionOpen) {
                        webSocket.send(
                            compileMessage(
                                ThunderRequest.Builder()
                                    .command(Command.CONNECT)
                                    .header(VERSION to SUPPORTED_VERSIONS)
                                    .build()
                            )
                        )
                    }
                    _events.tryEmit(it)
                }.launchIn(innerScope)
            }
        }
    }

    private fun closeConnection() = synchronized(this) {
        socket?.let {
            _socketState.update { ThunderState.ERROR() }
            if (it.close(1000, "shutdown")) socket = null
            _socketHeaderUUID.clear()
            if (::connectionJob.isInitialized) connectionJob.cancel()
        }
    }

    override fun send(message: com.jeremy.thunder.state.ThunderRequest) {
        val request = message as StompRequest
        valveCache.requestToValve(request)
        recoveryCache.set(request)
    }

    private fun subscribe(topic: String, payload: String) {
        socket?.let {
            runCatching {
                val uuid = UUID.randomUUID().toString()
                manageHeaderUuid(topic, uuid)
                it.send(
                    compileMessage(
                        ThunderRequest.Builder()
                            .command(Command.SUBSCRIBE)
                            .header(
                                ID to uuid,
                                DESTINATION to topic,
                                ACK to DEFAULT_ACK,
                            )
                            .payload(payload)
                            .build()
                    )
                )
            }
        }
    }

    private  fun unsubscribe(topic: String) {
        socket?.let {
            runCatching {
                val uuid = getHeaderUuid(topic)
                it.send(
                    compileMessage(
                        ThunderRequest.Builder()
                            .command(Command.SUBSCRIBE)
                            .header(
                                ID to uuid,
                                DESTINATION to topic,
                                ACK to DEFAULT_ACK,
                            )
                            .build()
                    )
                )
            }
        }
    }

    private fun manageHeaderUuid(topic: String, uuid: String) {
        if (_socketHeaderUUID.containsKey(topic) && !_socketHeaderUUID[topic].isNullOrEmpty()) {
            _socketHeaderUUID[topic]?.offer(uuid)
        } else {
            _socketHeaderUUID[topic] = LinkedList<String>().apply { offer(uuid) }
        }
    }

    private fun getHeaderUuid(topic: String): String {
        return if (_socketHeaderUUID.containsKey(topic) && !_socketHeaderUUID[topic].isNullOrEmpty()) {
            _socketHeaderUUID[topic]?.poll() ?: ""
        } else {
            ""
        }
    }

    class Factory: StateManager.Factory {
        override fun create(
            connectionListener: AppConnectionListener,
            networkStatus: NetworkConnectivityService,
            cacheController: CacheController,
            webSocketCore: WebSocket.Factory
        ): StateManager {
            return StompStateManager(
                connectionListener = connectionListener,
                networkState = networkStatus,
                recoveryCache = cacheController.rCache,
                valveCache = cacheController.vCache,
                webSocketCore = webSocketCore,
                scope = CoroutineScope(SupervisorJob())
            )
        }
    }

    companion object {
        private const val RETRY_CONNECTION_GAP = 1_000L
    }
}