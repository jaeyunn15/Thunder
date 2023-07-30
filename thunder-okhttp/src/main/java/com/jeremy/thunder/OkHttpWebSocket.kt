package com.jeremy.thunder

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class OkHttpWebSocket internal constructor(
    private val socketListener: SocketListener,
    private val socketHandler: SocketHandler,
    private val scope: CoroutineScope
) : WebSocket {

    private val _event = MutableStateFlow<WebSocketEvent?>(null)

    init {
        socketListener.collectEvent().onEach {
            when (it) {
                is WebSocketEvent.OnConnectionOpen -> {
                    socketHandler.open(it.webSocket as okhttp3.WebSocket)
                }
                else -> _event.tryEmit(it)
            }
        }.launchIn(scope)
    }

    override fun open(webSocket: okhttp3.WebSocket) {
        // ?
    }

    override fun events(): Flow<WebSocketEvent> {
        return _event.asSharedFlow().filterNotNull()
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
        private val socketListener: SocketListener,
        private val scope: CoroutineScope
    ) : WebSocket.Factory {
        override fun create(): WebSocket =
            OkHttpWebSocket(
                socketListener = socketListener,
                socketHandler = SocketHandler(),
                scope = scope
            )
    }
}