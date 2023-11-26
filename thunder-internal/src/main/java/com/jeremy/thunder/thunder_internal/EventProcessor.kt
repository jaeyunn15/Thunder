package com.jeremy.thunder.thunder_internal

import kotlinx.coroutines.flow.Flow

interface EventProcessor <T> {
    fun collectEvent(): Flow<T>
    suspend fun onEventDelivery(event: T)
}