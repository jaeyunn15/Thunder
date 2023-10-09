package com.jeremy.thunder.event

import com.jeremy.thunder.event.converter.Converter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * [EventMapper] - Convert WebSocketEvent to Generic type data.
* */

class EventMapper<T> constructor(
    private val converter: Converter<T>
) {

    fun mapEvent(flow: Flow<WebSocketEvent>): Flow<T?> = flow.filter {
        it is WebSocketEvent.OnMessageReceived
    }.map {
        (it as WebSocketEvent.OnMessageReceived).data
    }.map {
        try {
            val result = converter.convert(it)
            result
        } catch (e: Exception) {
            null
        }
    }

    fun mapEvent(event: WebSocketEvent): T? = if (event is WebSocketEvent.OnMessageReceived) {
        try {
            converter.convert(event.data)
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }

    class Factory {
        fun create(converter: Converter<*>): EventMapper<*> {
            return EventMapper(converter)
        }
    }
}