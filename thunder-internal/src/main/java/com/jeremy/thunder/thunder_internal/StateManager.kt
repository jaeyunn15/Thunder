package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import com.jeremy.thunder.thunder_internal.state.ManagerState
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
            cacheController: ICacheController<ThunderRequest>,
            webSocketCore: WebSocket.Factory
        ): StateManager
    }
}

