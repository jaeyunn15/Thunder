package com.jeremy.thunder

import com.jeremy.thunder.thunder_internal.WebSocket
import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart


class OkHttpWebSocket internal constructor(
    private val provider: ConnectionProvider,
    private val socketListener: SocketListener,
    private val socketHandler: SocketHandler,
) : WebSocket {


    override fun open(): Flow<WebSocketEvent> = socketListener.collectEvent()
        .onStart {
            provider.provide(socketListener)
        }.onEach {
            when (it) {
                is WebSocketEvent.OnConnectionOpen -> {
                    socketHandler.initWebSocket(it.webSocket as okhttp3.WebSocket)
                }
                else -> Unit
            }
        }

    override fun send(data: String): Boolean {
        return socketHandler.send(data)
    }

    override fun close(code: Int, reason: String): Boolean {
        return socketHandler.close(code, reason)
    }

    override fun cancel() {
        socketHandler.cancel()
    }


    class Factory(
        private val provider: ConnectionProvider,
    ) : WebSocket.Factory {
        override fun create(): WebSocket =
            OkHttpWebSocket(
                provider = provider ,
                socketListener = SocketListener(),
                socketHandler = SocketHandler(),
            )
    }
}