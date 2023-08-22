package com.jeremy.thunder.internal

import com.jeremy.thunder.CoroutineScope.scope
import com.jeremy.thunder.event.EventProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ThunderProvider internal constructor(
    private val thunderStateManager: ThunderStateManager,
    private val eventProcessor: EventProcessor<com.jeremy.thunder.core.WebSocketEvent>,
    scope: CoroutineScope
) {

    init {
        thunderStateManager.collectWebSocketEvent().onEach {
            eventProcessor.onEventDelivery(it)
        }.launchIn(scope)
    }

    fun observeEvent(): Flow<com.jeremy.thunder.core.WebSocketEvent> {
        return eventProcessor.collectEvent()
    }

    fun send(key: String, message: String) {
        thunderStateManager.send(key, message)
    }

    class Factory(
        private val thunderStateManager: ThunderStateManager,
        private val eventProcessor: EventProcessor<com.jeremy.thunder.core.WebSocketEvent>
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