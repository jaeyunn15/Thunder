package com.jeremy.thunder.stomp.compiler

import com.jeremy.thunder.stomp.model.DESTINATION
import com.jeremy.thunder.thunder_internal.Converter
import com.jeremy.thunder.thunder_internal.IMapper
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

class StompEventMapper<T> constructor(
    private val converter: Converter<T>,
    coroutineScope: CoroutineScope
) : IMapper<T> {
    private val messageCompiler = MessageCompiler
    private val _eventMappingChannel = MutableSharedFlow<WebSocketEvent>(
        replay = 1,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val mapToResultChannel = MutableSharedFlow<T>(
        replay = 1,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        _eventMappingChannel
            .filterIsInstance<WebSocketEvent.OnMessageReceived>()
            .map { it.data }
            .map(messageCompiler::parseMessage)
            .map(::handleMessage)
            .convertNonNullFlow()
            .map(mapToResultChannel::tryEmit)
            .launchIn(coroutineScope)
    }

    private fun <T> Flow<T?>.convertNonNullFlow(): Flow<T> = transform {
        if (it != null) return@transform emit(it)
    }

    private fun handleMessage(response: ThunderStompResponse): T? {
        return when (response.command) {
            ResponseCommandType.MESSAGE -> {
                val message = response as MessageResponse
                val dest = message.header[DESTINATION]
                if (dest != null && message.payload != null) {
                    converter.convert(message.payload)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    override fun mapEventToGeneric(event: WebSocketEvent) {
        _eventMappingChannel.tryEmit(event)
    }

    override fun mapEventFlow(): Flow<T> = mapToResultChannel

    class Factory : IMapper.Factory {
        override fun create(
            converter: Converter<*>,
            coroutineScope: CoroutineScope
        ): StompEventMapper<*> {
            return StompEventMapper(converter, coroutineScope)
        }
    }
}