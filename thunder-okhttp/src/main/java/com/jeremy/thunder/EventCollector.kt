package com.jeremy.thunder

import com.jeremy.thunder.event.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface EventCollector {
    fun collectEvent(): Flow<WebSocketEvent>
}