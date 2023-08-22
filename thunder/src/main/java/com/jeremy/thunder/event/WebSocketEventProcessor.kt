package com.jeremy.thunder.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
* Buffer Channel for consume Socket Event
* */

class WebSocketEventProcessor: EventProcessor<com.jeremy.thunder.core.WebSocketEvent> {

    private val channel = Channel<com.jeremy.thunder.core.WebSocketEvent>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun collectEvent(): Flow<com.jeremy.thunder.core.WebSocketEvent> {
        return channel.receiveAsFlow()
    }

    override suspend fun onEventDelivery(event: com.jeremy.thunder.core.WebSocketEvent) {
        channel.trySendBlocking(event)
    }

    //todo : dispose channel close event
}