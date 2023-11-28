package com.jeremy.thunder

import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class SocketHandler : com.jeremy.thunder.thunder_internal.WebSocket {

    private var socket: okhttp3.WebSocket? = null

    fun initWebSocket(socket: okhttp3.WebSocket) {
        this.socket = socket
    }

    override fun open(): Flow<WebSocketEvent> {
        return emptyFlow()
    }

    override fun send(data: String): Boolean {
        return socket?.send(data) ?: false
    }

    override fun close(code: Int, reason: String): Boolean {
        return socket?.close(code, reason).apply { socket = null } ?: false
    }

    override fun cancel() {
        socket?.cancel()
        socket = null
    }

}