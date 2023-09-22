package com.jeremy.thunder.internal

import com.google.gson.Gson
import com.jeremy.thunder.event.ConvertAdapter
import com.jeremy.thunder.event.SocketEventKeyStore
import com.jeremy.thunder.getAboutRawType
import com.jeremy.thunder.getParameterUpperBound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

/**
 * create logic by annotation
 * @Receive - create pipeline flow for observer
 * @Send - create send data
 * */

class ServiceExecutor internal constructor(
    private val thunderProvider: ThunderProvider,
    private val scope: CoroutineScope
) {

    fun executeReceive(method: Method, args: Array<out Any>): Any {
        require(method.genericReturnType != Flow::class.java) { "return type must flow" }

        return when (method.genericReturnType.getAboutRawType()) {
            Flow::class.java -> {
                method.requireParameterTypes { "Receive method must have zero parameter: $method" }
                method.requireReturnTypeIsOneOf(ParameterizedType::class.java) { "Receive method must return ParameterizedType: $method" }
                val returnType = (method.genericReturnType as ParameterizedType).getParameterUpperBound(0)
                val converter = ConvertAdapter.Factory().create(returnType)
                val eventMapper = SocketEventKeyStore().findEventMapper(returnType, method.annotations, converter)
                thunderProvider.observeEvent()
                    .map(eventMapper::mapEvent)
                    .filterNotNull()
                    .createPipeline()
                    .receiveFlow()
            }

            else -> require(false) { "Wrapper Type must be Flow." }
        }
    }

    private fun Flow<Any>.createPipeline(): ReceivePipeline<*> = ReceivePipeline(this, scope)

    fun executeSend(method: Method, args: Array<out Any>) {
        require(args.isNotEmpty()) { "@Send method require at least 1 arguments for execute service" }
        scope.launch(Dispatchers.Default) {
            val request = Gson().toJson(args[0])
            thunderProvider.send(request, request)
        }
    }

    class Factory(
        private val thunderProvider: ThunderProvider,
        private val scope: CoroutineScope
    ) {

        fun create(): ServiceExecutor {
            return ServiceExecutor(thunderProvider, scope)
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