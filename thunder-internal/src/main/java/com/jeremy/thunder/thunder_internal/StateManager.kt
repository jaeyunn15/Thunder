package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_internal.cache.ICacheController
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface StateManager {
    fun getStateOfType(): com.jeremy.thunder.thunder_state.ManagerState

    fun collectWebSocketEvent(): Flow<WebSocketEvent>

    fun openConnection(onConnect: ()-> Unit)

    fun closeConnection(onDisconnect: () -> Unit)

    fun retryConnection(onConnect: () -> Unit)

    /**
     * Either websocket & stomp always work well using send.
     * */
    fun send(message: ThunderRequest)

    interface Factory {
        fun create(
            connectionListener: AppConnectionListener,
            networkStatus: NetworkConnectivityService,
            cacheController: ICacheController<ThunderRequest>,
            webSocketCore: WebSocket.Factory
        ): StateManager
    }
}

