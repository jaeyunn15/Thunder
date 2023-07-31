package com.jeremy.thunder

import kotlinx.coroutines.flow.Flow

interface EventCollector {
    fun collectEvent(): Flow<com.jeremy.thunder.event.WebSocketEvent>
}