package com.jeremy.thunder

import okhttp3.OkHttpClient
import okhttp3.Request

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