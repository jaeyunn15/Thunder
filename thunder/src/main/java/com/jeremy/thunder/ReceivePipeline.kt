package com.jeremy.thunder

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class ReceivePipeline<T>(
    private val converter: Converter<T>,
    socketEventFlow: Flow<WebSocketEvent>,
    scope: CoroutineScope
) {
    init {
        socketEventFlow.onEach { event ->
            when (event) {
                is WebSocketEvent.OnMessageReceived -> {
                    store(event.data)
                }

                else -> Unit
            }
        }.launchIn(scope)
    }

    private val _cache = Channel<T>(onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun store(data: String) {
        _cache.trySendBlocking(converter.convert(data))
    }

    fun receiveFlow(): Flow<T> {
        return _cache.receiveAsFlow()
    }
}