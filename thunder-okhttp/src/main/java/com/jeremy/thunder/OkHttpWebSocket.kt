package com.jeremy.thunder

import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.ws.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
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

    override fun error(t: String) {
        // ?
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