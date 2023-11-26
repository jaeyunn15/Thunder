package com.jeremy.thunder

import okhttp3.OkHttpClient
import okhttp3.Request

fun OkHttpClient.makeWebSocketCore(url: String): com.jeremy.thunder.thunder_internal.WebSocket.Factory {
    return OkHttpWebSocket.Factory(
        provider = ConnectionProvider(this, url),
    )
}

interface SocketListenerProvider {
    fun provide(socketListener: SocketListener)
}

class ConnectionProvider(
    private val okHttpClient: OkHttpClient,
    private val url: String
) : SocketListenerProvider {

    override fun provide(socketListener: SocketListener) {
        okHttpClient.newWebSocket(Request.Builder().url(url).build(), socketListener)
    }
}