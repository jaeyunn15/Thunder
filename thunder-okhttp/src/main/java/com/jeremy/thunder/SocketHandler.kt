package com.jeremy.thunder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class SocketHandler : WebSocket {

    private var socket: okhttp3.WebSocket? = null

    override fun open(webSocket: okhttp3.WebSocket) {
        socket = webSocket
    }

    override fun events(): Flow<com.jeremy.thunder.event.WebSocketEvent> {
        return emptyFlow()
    }

    override fun send(data: String) {
        socket?.send(data)
    }

    override fun close(code: Int, reason: String) {
        socket?.close(code, reason)
    }

    override fun cancel() {
        socket?.cancel()
    }

    override fun error(t: String) {
        // socket already error
    }
}