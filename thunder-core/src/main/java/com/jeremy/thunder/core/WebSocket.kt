package com.jeremy.thunder.core

import kotlinx.coroutines.flow.Flow

interface WebSocket {

    //websocket 오픈 시
    fun open()

    fun events(): Flow<WebSocketEvent>

    //websocket 메세지 전송
    fun send(data: String): Boolean

    //websocket 연결 종료 - code & reason
    fun close(code: Int, reason: String)

    fun cancel()

    //websocket 연결 오류
    fun error(t: String)

    interface Factory{
        fun create(): WebSocket
    }
}