package com.jeremy.thunder.stomp.compiler

import com.jeremy.thunder.event.IMapper
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.event.converter.Converter
import com.jeremy.thunder.stomp.model.DESTINATION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

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
            .filter { it is WebSocketEvent.OnMessageReceived }
            .map { (it as WebSocketEvent.OnMessageReceived).data }
            .map(messageCompiler::parseMessage)
            .map(::handleMessage)
            .map {
                it?.let {
                    mapToResultChannel.tryEmit(it)
                }
            }
            .launchIn(coroutineScope)
    }

    private fun handleMessage(response: ThunderResponse): T? {
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

    class Factory: IMapper.Factory {
        override fun create(converter: Converter<*>, coroutineScope: CoroutineScope): StompEventMapper<*> {
            return StompEventMapper(converter, coroutineScope)
        }
    }
}