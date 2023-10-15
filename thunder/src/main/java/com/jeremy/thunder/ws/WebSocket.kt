package com.jeremy.thunder.ws

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface WebSocket {

    //websocket 오픈 시
    fun open()

    fun events(): Flow<WebSocketEvent>

    //Websocket send message
    fun send(data: String): Boolean

    //Websocket Connection close - code & reason
    fun close(code: Int, reason: String)

    fun cancel()

    //Websocket Connection failure
    fun error(t: String)

    interface Factory{
        fun create(): WebSocket
    }
}