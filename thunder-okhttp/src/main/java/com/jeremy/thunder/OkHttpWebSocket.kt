package com.jeremy.thunder

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class OkHttpWebSocket internal constructor(
    private val socketListener: SocketListener,
    private val socketHandler: SocketHandler
) : WebSocket {

    init {
        socketListener.collectEvent().onEach {
            Log.d("SocketListener::", "$it")
            when (it) {
                is WebSocketEvent.OnConnectionOpen -> {
                    socketHandler.open(it.webSocket as okhttp3.WebSocket)
                }
                else -> Unit
            }
        }.launchIn(GlobalScope)
    }

    override fun open(webSocket: okhttp3.WebSocket) {
        // ?
    }

    override fun send(data: String) {
        socketHandler.send(data)
    }

    override fun close(code: Int, reason: String) {
        socketHandler.close(code, reason)
    }

    override fun cancel() {
        socketHandler.cancel()
    }

    override fun error(t: String) {
        // ?
    }

    class Factory(
        private val socketListener: SocketListener
    ) : WebSocket.Factory {
        override fun create(): WebSocket =
            OkHttpWebSocket(
                socketListener = socketListener,
                socketHandler = SocketHandler()
            )
    }
}