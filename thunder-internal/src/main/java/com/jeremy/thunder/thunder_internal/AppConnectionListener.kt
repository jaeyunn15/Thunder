package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_state.AppState
import kotlinx.coroutines.flow.Flow

interface AppConnectionListener {
    fun collectAppState(): Flow<AppState>
}