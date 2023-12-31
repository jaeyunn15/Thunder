package com.jeremy.thunder.cache

import com.jeremy.thunder.thunder_internal.cache.BaseValve
import com.jeremy.thunder.thunder_internal.event.ThunderRequest
import com.jeremy.thunder.thunder_state.ConnectState
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

class ValveCache: BaseValve<ThunderRequest> {
    private val isEmissiable = MutableStateFlow<Boolean>(true)

    private val innerQueue = ConcurrentLinkedQueue<ThunderRequest>()

    private fun cacheFlow(): Flow<List<ThunderRequest>> = flow {
        while (currentCoroutineContext().isActive) {
            if (isEmissiable.value && innerQueue.isNotEmpty()) {
                val emitCacheList = mutableListOf<ThunderRequest>()
                while (innerQueue.isNotEmpty()) {
                    innerQueue.poll()?.let { emitCacheList.add(it) }
                }
                emit(emitCacheList)
            }
            delay(CACHE_EMIT_GAP)
        }
    }

    override fun onUpdateValve(state: ConnectState) {
        isEmissiable.update { state is ConnectState.Establish }
    }

    override fun requestToValve(request: ThunderRequest) {
        val lastRequest = innerQueue.poll()
        if (lastRequest != request) {
            innerQueue.add(request)
        }
    }

    override fun emissionOfValveFlow(): Flow<List<ThunderRequest>> = cacheFlow()

    companion object {
        private const val CACHE_EMIT_GAP = 500L
    }

    class Factory: BaseValve.BaseValveFactory<ThunderRequest> {
        override fun create(): ValveCache {
            return ValveCache()
        }
    }
}