package com.jeremy.thunder.ws

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface WebSocket {

    //websocket open
    fun open(): Flow<WebSocketEvent>

    //Websocket send message
    fun send(data: String): Boolean

    //Websocket Connection close - code & reason
    fun close(code: Int, reason: String): Boolean

    fun cancel()

    interface Factory{
        fun create(): WebSocket
    }
}