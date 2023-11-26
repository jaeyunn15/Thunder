package com.jeremy.thunder.thunder_internal.event

sealed class WebSocketEvent {
    data class OnConnectionOpen(
        val webSocket: Any
    ): WebSocketEvent()

    data class OnMessageReceived(
        val data: String
    ): WebSocketEvent()

    data class OnConnectionError(
        val error: String?
    ): WebSocketEvent()

    object OnConnectionClosed: WebSocketEvent()
}