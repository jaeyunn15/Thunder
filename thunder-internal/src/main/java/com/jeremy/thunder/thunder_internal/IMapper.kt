package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface IMapper<T> {
    fun mapEventToGeneric(event: WebSocketEvent)

    fun mapEventFlow(): Flow<T>

    interface Factory {
        fun create(converter: Converter<*>, coroutineScope: CoroutineScope): IMapper<*>
    }
}