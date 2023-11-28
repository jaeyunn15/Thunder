package com.jeremy.thunder

import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface EventCollector {
    fun collectEvent(): Flow<WebSocketEvent>
}