package com.jeremy.thunder.event

import com.jeremy.thunder.event.converter.Converter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

/**
 * [EventMapper] - Convert WebSocketEvent to Generic type data.
 * */

class EventMapper<T> constructor(
    private val converter: Converter<T>,
    coroutineScope: CoroutineScope
) {
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
        _eventMappingChannel.filter { it is WebSocketEvent.OnMessageReceived }
            .map { (it as WebSocketEvent.OnMessageReceived).data }.map(converter::convert)
            .map(mapToResultChannel::tryEmit).launchIn(coroutineScope)
    }

    fun mapEventToGeneric(event: WebSocketEvent) {
        _eventMappingChannel.tryEmit(event)
    }

    fun mapEventFlow(): Flow<T> = mapToResultChannel

    class Factory {
        fun create(converter: Converter<*>, coroutineScope: CoroutineScope): EventMapper<*> {
            return EventMapper(converter, coroutineScope)
        }
    }
}