package com.jeremy.thunder

import com.jeremy.thunder.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart


class OkHttpWebSocket internal constructor(
    private val provider: ConnectionProvider,
    private val socketListener: SocketListener,
    private val socketHandler: SocketHandler,
    private val scope: CoroutineScope
) : com.jeremy.thunder.WebSocket {

    private val _event = MutableStateFlow<com.jeremy.thunder.WebSocketEvent?>(null)
    override fun open() {
        socketListener.collectEvent().onStart {
            provider.provide(socketListener)
        }.onEach {
            _event.tryEmit(it)
            when (it) {
                is com.jeremy.thunder.WebSocketEvent.OnConnectionOpen -> {
                    socketHandler.initWebSocket(it.webSocket as okhttp3.WebSocket)
                }
                else -> Unit
            }
        }.launchIn(scope)
    }

    override fun events(): Flow<com.jeremy.thunder.WebSocketEvent> {
        return _event.asSharedFlow().filterNotNull()
    }

    override fun send(data: String): Boolean {
        return socketHandler.send(data)
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
        private val provider: ConnectionProvider,
        private val scope: CoroutineScope
    ) : com.jeremy.thunder.WebSocket.Factory {
        override fun create(): com.jeremy.thunder.WebSocket =
            OkHttpWebSocket(
                provider = provider ,
                socketListener = SocketListener(),
                socketHandler = SocketHandler(),
                scope = scope
            )
    }
}