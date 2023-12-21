package com.jeremy.thunder

import android.app.Application
import android.content.Context
import com.jeremy.thunder.cache.CacheController
import com.jeremy.thunder.connection.AppConnectionProvider
import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.event.EventMapper
import com.jeremy.thunder.event.WebSocketEventProcessor
import com.jeremy.thunder.event.converter.ConverterType
import com.jeremy.thunder.internal.ServiceExecutor
import com.jeremy.thunder.internal.ThunderProvider
import com.jeremy.thunder.internal.ThunderStateManager
import com.jeremy.thunder.network.NetworkConnectivityServiceImpl
import com.jeremy.thunder.thunder_internal.EventProcessor
import com.jeremy.thunder.thunder_internal.IMapper
import com.jeremy.thunder.thunder_internal.StateManager
import com.jeremy.thunder.thunder_internal.WebSocket
import com.jeremy.thunder.thunder_state.WebSocketEvent
import com.jeremy.thunder.ws.Receive
import com.jeremy.thunder.ws.Send
import com.jeremy.thunder.ws.stomp.StompSend
import com.jeremy.thunder.ws.stomp.StompSubscribe
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

    private val invocationHandler = InvocationHandler { _, method, nullableArgs ->
        method.annotations.getOrNull(0)?.let { annotation ->
            val args = nullableArgs ?: arrayOf()
            return@InvocationHandler when (annotation) {
                is Send -> serviceExecutor.executeSend(method, args)
                is Receive -> serviceExecutor.executeReceive(method, args)
                is StompSend -> serviceExecutor.executeStompSend(method, args)
                is StompSubscribe -> serviceExecutor.executeStompSubscribe(method, args)
                else -> require(false) { "there is no matching annotation" }
            }
        }
    }

    class Builder {
        private var webSocketCore: WebSocket.Factory? = null
        private val appConnectionProvider by lazy { AppConnectionProvider() }
        private var stateManager: StateManager? = null
        private var iMapperFactory: IMapper.Factory? = null
        private var context: Context? = null
        private var converterType: ConverterType = ConverterType.Serialization

        fun setWebSocketFactory(core: WebSocket.Factory): Builder = apply { this.webSocketCore = core }

        fun setApplicationContext(context: Context): Builder = apply {
            this.context = context
            (this.context as Application).registerActivityLifecycleCallbacks(appConnectionProvider)
        }

        fun setConverterType(type: ConverterType) = apply {
            converterType = type
        }

        fun setStateManager(manager: StateManager.Factory) = apply {
            stateManager = manager.create(
                connectionListener = appConnectionProvider,
                networkStatus = createNetworkConnectivity(),
                cacheController = createCacheController(),
                webSocketCore = checkNotNull(webSocketCore)
            )
        }

        fun setEventMapper(mapper: IMapper.Factory) = apply {
            iMapperFactory = mapper
        }

        private fun createCacheController(): CacheController {
            return CacheController.Factory().create()
        }

        private fun createNetworkConnectivity(): com.jeremy.thunder.thunder_internal.NetworkConnectivityService {
            require(context != null) { "Application Context should be set before request build()" }
            return NetworkConnectivityServiceImpl(checkNotNull(context))
        }


        fun build(): Thunder {
            return Thunder(
                webSocketCore = checkNotNull(webSocketCore),
                serviceExecutor = createServiceExecutor(),
                scope = scope
            )
        }

        private fun createServiceExecutor(): ServiceExecutor {
            checkEventMapperFactory()
            return ServiceExecutor.Factory(
                thunderProvider = createThunderProvider(),
                converterType = converterType,
                iMapperFactory = requireNotNull(iMapperFactory),
                scope = scope
            ).create()
        }

        private fun checkEventMapperFactory() {
            if (iMapperFactory == null) {
                iMapperFactory = EventMapper.Factory()
            }
        }

        private fun createThunderProvider(): ThunderProvider {
            checkStateManager()
            return ThunderProvider.Factory(
                thunderStateManager = checkNotNull(stateManager),
                eventProcessor = createEventProcessor()
            ).create()
        }

        private fun createEventProcessor(): EventProcessor<WebSocketEvent> {
            return WebSocketEventProcessor()
        }

        private fun checkStateManager() {
            if (stateManager == null) {
                createDefaultStateManager()
            }
        }

        private fun createDefaultStateManager() {
            stateManager = ThunderStateManager.Factory()
                .create(
                    connectionListener = appConnectionProvider,
                    networkStatus = createNetworkConnectivity(),
                    cacheController = createCacheController(),
                    webSocketCore = checkNotNull(webSocketCore)
                )
        }
    }
}