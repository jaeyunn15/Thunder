package com.jeremy.thunder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.Request

fun OkHttpClient.makeWebSocketCore(url: String): WebSocket.Factory {
    val socketListener = SocketListener()
    newWebSocket(
        request = Request.Builder().url(url).build(),
        listener = socketListener
    )
    return OkHttpWebSocket.Factory(
        socketListener = socketListener,
        scope = CoroutineScope(SupervisorJob())
    )
}