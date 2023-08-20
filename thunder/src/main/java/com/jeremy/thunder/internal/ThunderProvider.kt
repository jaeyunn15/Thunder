package com.jeremy.thunder.internal

import com.jeremy.thunder.CoroutineScope.scope
import com.jeremy.thunder.event.EventProcessor
import com.jeremy.thunder.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ThunderProvider internal constructor(
    private val thunderStateManager: ThunderStateManager,
    private val eventProcessor: EventProcessor<WebSocketEvent>,
    scope: CoroutineScope
) {

    init {
        thunderStateManager.collectWebSocketEvent().onEach {
            eventProcessor.onEventDelivery(it)
        }.launchIn(scope)
    }

    fun observeEvent(): Flow<WebSocketEvent> {
        return eventProcessor.collectEvent()
    }

    fun send(key: String, message: String) {
        thunderStateManager.send(key, message)
    }

    class Factory(
        private val thunderStateManager: ThunderStateManager,
        private val eventProcessor: EventProcessor<WebSocketEvent>
    ) {

        fun create(): ThunderProvider {
            return ThunderProvider(
                thunderStateManager,
                eventProcessor,
                scope
            )
        }
    }
}