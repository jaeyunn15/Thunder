package com.jeremy.thunder.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class ReceivePipeline<T>(
    socketEventFlow: Flow<T>,
    scope: CoroutineScope
) {
    init {
        socketEventFlow.onEach(::store).launchIn(scope)
    }

    private val _cache = Channel<T>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun store(data: T) {
        _cache.trySendBlocking(data).onFailure { }.onClosed { }

    }

    fun receiveFlow(): Flow<T> {
        return _cache.receiveAsFlow()
    }
}