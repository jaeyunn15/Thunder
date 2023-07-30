package com.jeremy.thunder

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SocketListener : WebSocketListener(), EventCollector {

    private val _eventFlow = MutableSharedFlow<WebSocketEvent>(
        replay = 1,
        extraBufferCapacity = DEFAULT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun collectEvent(): Flow<WebSocketEvent> {
        return _eventFlow.asSharedFlow()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        _eventFlow.tryEmit(WebSocketEvent.OnConnectionOpen(webSocket))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        _eventFlow.tryEmit(WebSocketEvent.OnMessageReceived(bytes.toString()))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        _eventFlow.tryEmit(WebSocketEvent.OnMessageReceived(text))
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        _eventFlow.tryEmit(WebSocketEvent.OnConnectionClosed)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        _eventFlow.tryEmit(WebSocketEvent.OnConnectionError(t.message))
    }

    companion object {
        private const val DEFAULT_BUFFER = 120
    }
}