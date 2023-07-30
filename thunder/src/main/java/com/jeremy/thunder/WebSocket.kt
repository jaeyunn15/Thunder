package com.jeremy.thunder

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface WebSocket {

    //websocket 오픈 시
    fun open(webSocket: okhttp3.WebSocket)

    fun events(): Flow<WebSocketEvent>

    //websocket 메세지 전송
    fun send(data: String)

    //websocket 연결 종료 - code & reason
    fun close(code: Int, reason: String)

    fun cancel()

    //websocket 연결 오류
    fun error(t: String)

    interface Factory{
        fun create(): WebSocket
    }
}