package com.jeremy.thunder.internal

import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.connection.AppConnectionListener
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.network.NetworkConnectivityService
import com.jeremy.thunder.state.ManagerState
import com.jeremy.thunder.state.ThunderRequest
import com.jeremy.thunder.ws.WebSocket
import kotlinx.coroutines.flow.Flow

interface StateManager {
    fun getStateOfType(): ManagerState

    fun collectWebSocketEvent(): Flow<WebSocketEvent>

    /**
     * Either websocket & stomp always work well using send.
     * */
    fun send(message: ThunderRequest)

    interface Factory {
        fun create(
            connectionListener: AppConnectionListener,
            networkStatus: NetworkConnectivityService,
            cacheController: CacheController,
            webSocketCore: WebSocket.Factory
        ): StateManager
    }
}

