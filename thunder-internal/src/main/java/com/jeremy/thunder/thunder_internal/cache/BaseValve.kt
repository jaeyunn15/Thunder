package com.jeremy.thunder.thunder_internal.cache

import com.jeremy.thunder.thunder_state.ConnectState
import kotlinx.coroutines.flow.Flow

interface BaseValve<T> {

    fun onUpdateValve(state: ConnectState)
    fun requestToValve(request: T)
    fun emissionOfValveFlow(): Flow<List<T>>

    interface BaseValveFactory<T> {
        fun create(): BaseValve<T>
    }
}