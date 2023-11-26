package com.jeremy.thunder.event

import com.jeremy.thunder.thunder_internal.Converter
import com.jeremy.thunder.thunder_internal.IMapper
import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

/**
 * [EventMapper] - Convert WebSocketEvent to Generic type data.
 * */

class EventMapper<T> constructor(
    private val converter: Converter<T>,
    coroutineScope: CoroutineScope
) : IMapper<T> {
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
            .map { it.data}
            .map(converter::convert)
            .map(mapToResultChannel::tryEmit)
            .launchIn(coroutineScope)
    }

    override fun mapEventToGeneric(event: WebSocketEvent) {
        _eventMappingChannel.tryEmit(event)
    }

    override fun mapEventFlow(): Flow<T> = mapToResultChannel

    class Factory: IMapper.Factory {
        override fun create(converter: Converter<*>, coroutineScope: CoroutineScope): EventMapper<*> {
            return EventMapper(converter, coroutineScope)
        }
    }
}