package com.jeremy.thunder.internal

import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.thunder_internal.EventProcessor
import com.jeremy.thunder.thunder_internal.StateManager
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ThunderProvider internal constructor(
    private val stateManager: StateManager,
    private val eventProcessor: EventProcessor<WebSocketEvent>,
    scope: CoroutineScope
) {

    init {
        stateManager.collectWebSocketEvent().onEach(eventProcessor::onEventDelivery).launchIn(scope)
    }

    fun observeEvent(): Flow<WebSocketEvent> = eventProcessor.collectEvent()

    fun send(request: ThunderRequest) {
        stateManager.send(request)
    }

    fun subscribe(request: ThunderRequest) {
        stateManager.send(request) // Parts that need to be modified to use other dedicated methods
    }

    class Factory(
        private val thunderStateManager: StateManager,
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