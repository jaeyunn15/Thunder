package com.jeremy.thunder

import com.jeremy.thunder.ws.WebSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class SocketHandler : WebSocket {

    private var socket: okhttp3.WebSocket? = null

    fun initWebSocket(socket: okhttp3.WebSocket) {
        this.socket = socket
    }

    override fun open() {

    }

    override fun events(): Flow<com.jeremy.thunder.event.WebSocketEvent> {
        return emptyFlow()
    }

    override fun send(data: String): Boolean {
        return socket?.send(data) ?: false
    }

    override fun close(code: Int, reason: String) {
        socket?.close(code, reason)
        socket = null
    }

    override fun cancel() {
        socket?.cancel()
        socket = null
    }

    override fun error(t: String) {
        // socket already error
    }
}