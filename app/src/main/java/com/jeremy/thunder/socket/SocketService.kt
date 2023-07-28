package com.jeremy.thunder.socket

import com.jeremy.thunder.ws.Receive
import com.jeremy.thunder.ws.Send

interface SocketService {
    @Send
    fun request(request: List<Request>)

    @Receive
    fun response(): String
}