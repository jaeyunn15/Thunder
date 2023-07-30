package com.jeremy.thunder.event

import kotlinx.coroutines.flow.Flow

interface EventProcessor <T> {
    fun collectEvent(): Flow<T>
    suspend fun onEventDelivery(event: T)
}