package com.jeremy.thunder.internal

import com.jeremy.thunder.coroutine.CoroutineScope.scope
import com.jeremy.thunder.event.EventProcessor
import com.jeremy.thunder.event.WebSocketEvent
import com.jeremy.thunder.state.StompManager
import com.jeremy.thunder.state.StompRequest
import com.jeremy.thunder.state.ThunderManager
import com.jeremy.thunder.state.ThunderRequest
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
            ThunderManager -> {
                stateManager.send(request)
            }
            StompManager -> {
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