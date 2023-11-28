package com.jeremy.thunder.internal

import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.thunder_internal.EventProcessor
import com.jeremy.thunder.thunder_internal.event.StompRequest
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_state.WebSocketEvent
import com.jeremy.thunder.thunder_internal.StateManager
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
        stateManager.collectWebSocketEvent().onEach {
            eventProcessor.onEventDelivery(it)
        }.launchIn(scope)
    }

    fun observeEvent(): Flow<WebSocketEvent> {
        return eventProcessor.collectEvent()
    }

    fun send(request: ThunderRequest) {
        when(stateManager.getStateOfType()) {
            com.jeremy.thunder.thunder_state.ThunderManager -> {
                stateManager.send(request)
            }
            com.jeremy.thunder.thunder_state.StompManager -> {
                stateManager.send(request)
            }
        }
    }

    fun subscribe(request: StompRequest) {
        stateManager.send(request)
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