package com.jeremy.thunder.internal

import com.google.gson.Gson
import com.jeremy.thunder.event.SocketEventKeyStore
import com.jeremy.thunder.event.converter.ConverterType
import com.jeremy.thunder.event.converter.GsonConvertAdapter
import com.jeremy.thunder.event.converter.SerializeConvertAdapter
import com.jeremy.thunder.getAboutRawType
import com.jeremy.thunder.getParameterUpperBound
import com.jeremy.thunder.thunder_internal.Converter
import com.jeremy.thunder.thunder_internal.IMapper
import com.jeremy.thunder.thunder_internal.event.StompSendRequest
import com.jeremy.thunder.thunder_internal.event.StompSubscribeRequest
import com.jeremy.thunder.thunder_internal.event.WebSocketRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * create logic by annotation
 * @Receive - create pipeline flow for observer
 * @Send - create send data
 * @Subscribe - create subscribe for stomp
 * */

class ServiceExecutor internal constructor(
    private val thunderProvider: ThunderProvider,
    private val converterType: ConverterType,
    private val iMapperFactory: IMapper.Factory,
    private val scope: CoroutineScope
) {

    fun executeReceive(method: Method, args: Array<out Any>): Any {
        require(method.genericReturnType != Flow::class.java) { "return type must flow" }
        return when (method.genericReturnType.getAboutRawType()) {
            Flow::class.java -> {
                method.requireParameterTypes { "Receive method must have zero parameter: $method" }
                method.requireReturnTypeIsOneOf(ParameterizedType::class.java) { "Receive method must return ParameterizedType: $method" }
                val returnType = (method.genericReturnType as ParameterizedType).getParameterUpperBound(0)
                val converter = checkConverterType(converterType, returnType)
                val eventMapper = SocketEventKeyStore().findEventMapper(returnType, method.annotations, converter, scope, iMapperFactory)
                thunderProvider.observeEvent().onEach { eventMapper.mapEventToGeneric(it) }.launchIn(scope)
                eventMapper.mapEventFlow().filterNotNull().createPipeline().receiveFlow()
            }

            else -> require(false) { "Wrapper Type must be Flow." }
        }
    }

    private fun checkConverterType(converterType: ConverterType, returnType: Type): Converter<out Any?> {
        return when(converterType) {
            ConverterType.Gson -> GsonConvertAdapter.Factory().create(returnType)
            ConverterType.Serialization -> SerializeConvertAdapter.Factory().create(returnType)
        }
    }

    private fun Flow<Any>.createPipeline(): ReceivePipeline<*> = ReceivePipeline(this, scope)

    fun executeSend(method: Method, args: Array<out Any>) {
        scope.launch(Dispatchers.Default) {
            val request = Gson().toJson(args[0])
            thunderProvider.send(WebSocketRequest(request))
        }
    }

    fun executeStompSend(method: Method, args: Array<out Any>) {
        scope.launch(Dispatchers.Default) {
            require(args[0] is StompSendRequest) { "@Send with @Stomp annotation method require StompSendRequest type of first field. " }
            val request = args[0] as StompSendRequest
            thunderProvider.send(request)
        }
    }

    /**
     * This method exists for STOMP support.
     * */
    fun executeStompSubscribe(method: Method, args: Array<out Any>) {
        scope.launch(Dispatchers.Default) {
            val subscribeRequest = args[0] as StompSubscribeRequest
            thunderProvider.subscribe(subscribeRequest)
        }
    }

    class Factory(
        private val thunderProvider: ThunderProvider,
        private val converterType: ConverterType,
        private val iMapperFactory: IMapper.Factory,
        private val scope: CoroutineScope,
    ) {

        fun create(): ServiceExecutor {
            return ServiceExecutor(thunderProvider, converterType, iMapperFactory, scope)
        }
    }

    companion object {
        private inline fun Method.requireParameterTypes(
            vararg types: Class<*>,
            lazyMessage: () -> Any
        ) {
            require(genericParameterTypes.size == types.size, lazyMessage)
            require(
                genericParameterTypes.zip(types).all { (t1, t2) -> t2 === t1 || t2.isInstance(t1) },
                lazyMessage
            )
        }

        private inline fun Method.requireReturnTypeIsOneOf(
            vararg types: Class<*>,
            lazyMessage: () -> Any
        ) =
            require(
                types.any { it === genericReturnType || it.isInstance(genericReturnType) },
                lazyMessage
            )
    }
}