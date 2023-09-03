package com.jeremy.thunder.cache

import com.jeremy.thunder.state.ThunderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentLinkedQueue

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

    private val innerQueue = ConcurrentLinkedQueue<Pair<String,String>>()

    private fun cacheFlow(): Flow<List<String>> {
        return flow {
            while (currentCoroutineContext().isActive) {
                if (isEmissiable.value && innerQueue.isNotEmpty()) {
                    val emitCacheList = mutableListOf<String>()
                    while (innerQueue.isNotEmpty()) {
                        innerQueue.poll()?.let { emitCacheList.add(it.second) }
                    }
                    emit(emitCacheList)
                }
                delay(300)
            }
        }
    }

    fun onUpdateValveState(state: ThunderState) {
        isEmissiable.update { state == ThunderState.CONNECTED }
    }

    fun requestToValve(request: Pair<String, String>) {
        innerQueue.add(request)
    }

    fun emissionOfValveFlow(): Flow<List<String>> = cacheFlow()

    class Factory(
        private val scope: CoroutineScope
    ) {
        fun create(): ValveCache {
            return ValveCache(scope)
        }
    }
}