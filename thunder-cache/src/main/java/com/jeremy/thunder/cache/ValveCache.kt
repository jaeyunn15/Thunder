package com.jeremy.thunder.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A cache that locks the valve if the current state of the ThunderState is not Connected,
 * and opens and exports the valve when it reaches Connected.
 *
 * This exists to automatically manage socket requests when you make them, regardless of the current state of Thunder.
 * */

class ValveCache(
    scope: CoroutineScope
) {
    private val isEmissiable = MutableStateFlow<Boolean>(true)

    private val valveCache = MutableSharedFlow<Pair<String,String>>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    fun onUpdateValveState(state: com.jeremy.thunder.core.ThunderState) {
        if (state == com.jeremy.thunder.core.ThunderState.CONNECTED) {
            isEmissiable.update { true }
        } else {
            isEmissiable.update { false }
        }
    }

    fun requestToValve(request: Pair<String, String>) {
        valveCache.tryEmit(request)
    }

    fun emissionOfValveFlow(): Flow<List<String>> = combine(
        isEmissiable,
        valveCache.map { it.second }
    ) { isEmissiable, cache ->
        if (isEmissiable) {
            listOf(cache)
        } else {
            emptyList()
        }
    }

    class Factory(
        private val scope: CoroutineScope
    ) {
        fun create(): ValveCache {
            return ValveCache(scope)
        }
    }
}