package com.jeremy.thunder.thunder_internal

import kotlinx.coroutines.flow.Flow

interface AppConnectionListener {
    fun collectAppState(): Flow<com.jeremy.thunder.thunder_state.AppState>
}