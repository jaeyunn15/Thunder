package com.jeremy.thunder.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * mapEvent 내에서 convert가 실패하면 다른 응답이기에 실패했다고 봐야 하는지?
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

    class Factory {
        fun create(converter: Converter<*>): EventMapper<*> {
            return EventMapper(converter)
        }
    }
}