package com.jeremy.thunder

class SocketHandler : WebSocket {

    private var socket: okhttp3.WebSocket? = null

    override fun open(webSocket: okhttp3.WebSocket) {
        // socket already open
        socket = webSocket
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