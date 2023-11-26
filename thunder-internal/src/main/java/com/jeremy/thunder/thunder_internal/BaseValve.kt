package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_internal.state.ThunderState
import kotlinx.coroutines.flow.Flow

interface BaseValve<T> {

    fun onUpdateValveState(state: ThunderState)
    fun requestToValve(request: T)
    fun emissionOfValveFlow(): Flow<List<T>>

    interface BaseValveFactory<T> {
        fun create(): BaseValve<T>
    }
}