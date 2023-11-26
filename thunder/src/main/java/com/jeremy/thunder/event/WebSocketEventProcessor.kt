package com.jeremy.thunder.event

import com.jeremy.thunder.thunder_internal.EventProcessor
import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
* Buffer Channel for consume Socket Event
* */

class WebSocketEventProcessor: EventProcessor<WebSocketEvent> {

    private val channel = Channel<WebSocketEvent>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun collectEvent(): Flow<WebSocketEvent> {
        return channel.receiveAsFlow()
    }

    override suspend fun onEventDelivery(event: WebSocketEvent) {
        channel.trySendBlocking(event)
    }

}