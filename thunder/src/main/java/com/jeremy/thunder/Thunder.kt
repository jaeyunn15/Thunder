package com.jeremy.thunder

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.jeremy.thunder.network.NetworkConnectivityService
import com.jeremy.thunder.network.NetworkConnectivityServiceImpl
import com.jeremy.thunder.ws.Receive
import com.jeremy.thunder.ws.Send
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.contracts.contract

class Thunder private constructor(
    private val webSocketCore: WebSocket.Factory,
    private val thunderConnection: ThunderConnection
) {

    inline fun <reified T : Any> create(): T = create(T::class.java)

    fun <T> create(service: Class<T>): T = implementService(service)

    private fun <T> implementService(serviceInterface: Class<T>): T {
        val proxy = Proxy.newProxyInstance(
            serviceInterface.classLoader,
            arrayOf(serviceInterface),
            invocationHandler
        )
        return serviceInterface.cast(proxy)
    }

    private val invocationHandler = InvocationHandler { proxy, method, nullableArgs ->
        method.annotations.getOrNull(0)?.let { annotation ->
            val args = nullableArgs ?: arrayOf()
            return@InvocationHandler when (annotation) {
                is Send -> {
                    val request = Gson().toJson(args[0])
                    thunderConnection.socket.send(request)
                }

                is Receive -> {

                }

                else -> this
            }
        }
    }

    class Builder() {
        private var webSocketCore: WebSocket.Factory? = null
        private var thunderConnection: ThunderConnection? = null
        private var context: Context? = null

        fun webSocketCore(core: WebSocket.Factory): Builder = apply { this.webSocketCore = core }

        fun setApplicationContext(context: Context): Builder = apply { this.context = context }

        private fun createThunderConnection(): ThunderConnection {
            thunderConnection = ThunderConnection.Factory(
                createNetworkConnectivity(),
                checkNotNull(webSocketCore)
            ).create()
            return checkNotNull(thunderConnection)
        }

        private fun createNetworkConnectivity(): NetworkConnectivityService {
            return NetworkConnectivityServiceImpl(checkNotNull(context))
        }

        fun build(): Thunder {
            createThunderConnection()
            return Thunder(
                webSocketCore = checkNotNull(webSocketCore),
                thunderConnection = checkNotNull(thunderConnection)
            )
        }
    }
}