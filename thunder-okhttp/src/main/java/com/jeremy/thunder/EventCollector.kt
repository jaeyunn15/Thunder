package com.jeremy.thunder

import com.jeremy.thunder.thunder_internal.event.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface EventCollector {
    fun collectEvent(): Flow<WebSocketEvent>
}