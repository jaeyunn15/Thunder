package com.jeremy.thunder

import android.app.Application
import android.content.Context
import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.connection.AppConnectionProvider
import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.event.EventProcessor
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.event.WebSocketEventProcessor
import com.jeremy.thunder.event.converter.ConverterType
import com.jeremy.thunder.internal.ServiceExecutor
import com.jeremy.thunder.internal.ThunderProvider
import com.jeremy.thunder.internal.ThunderStateManager
import com.jeremy.thunder.network.NetworkConnectivityService
import com.jeremy.thunder.network.NetworkConnectivityServiceImpl
import com.jeremy.thunder.ws.Event
import com.jeremy.thunder.ws.Receive
import com.jeremy.thunder.ws.Send
import com.jeremy.thunder.ws.WebSocket
import kotlinx.coroutines.CoroutineScope
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class Thunder private constructor(
    private val webSocketCore: WebSocket.Factory,
    serviceExecutor: ServiceExecutor,
    private val scope: CoroutineScope
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
                is Event -> serviceExecutor.executeEvent(method, args)
                is Send -> serviceExecutor.executeSend(method, args)
                is Receive -> serviceExecutor.executeReceive(method, args)
                else -> require(false) { "there is no matching annotation" }
            }
        }
    }

    class Builder {
        private var webSocketCore: WebSocket.Factory? = null
        private var thunderStateManager: ThunderStateManager? = null
        private var context: Context? = null
        private val appConnectionProvider by lazy { AppConnectionProvider() }
        private var converterType: ConverterType = ConverterType.Serialization

        fun webSocketCore(core: WebSocket.Factory): Builder = apply { this.webSocketCore = core }

        fun setApplicationContext(context: Context): Builder = apply {
            this.context = context
            (this.context as Application).registerActivityLifecycleCallbacks(appConnectionProvider)
        }

        fun setConverterType(type: ConverterType) = apply {
            converterType = type
        }

        private fun createThunderStateManager(): ThunderStateManager {
            thunderStateManager = ThunderStateManager.Factory(
                connectionListener = appConnectionProvider,
                networkStatus = createNetworkConnectivity(),
                cacheController = createCacheController(),
                webSocketCore = checkNotNull(webSocketCore)
            ).create()
            return checkNotNull(thunderStateManager)
        }

        private fun createCacheController(): CacheController {
            return CacheController.Factory(
                scope = scope
            ).create()
        }

        private fun createNetworkConnectivity(): NetworkConnectivityService {
            require(context != null) { "Application Context should be set before request build()" }
            return NetworkConnectivityServiceImpl(checkNotNull(context))
        }

        private fun createEventProcessor(): EventProcessor<WebSocketEvent> {
            return WebSocketEventProcessor()
        }

        private fun createThunderProvider(): ThunderProvider {
            require(thunderStateManager != null) { "ThunderStateManager should not be null" }
            return ThunderProvider.Factory(
                thunderStateManager = checkNotNull(thunderStateManager),
                eventProcessor = createEventProcessor()
            ).create()
        }

        private fun createServiceExecutor(): ServiceExecutor {
            return ServiceExecutor.Factory(
                thunderProvider = createThunderProvider(),
                converterType = converterType,
                scope = scope
            ).create()
        }

        fun build(): Thunder {
            createThunderStateManager()
            return Thunder(
                webSocketCore = checkNotNull(webSocketCore),
                serviceExecutor = createServiceExecutor(),
                scope = scope
            )
        }
    }
}