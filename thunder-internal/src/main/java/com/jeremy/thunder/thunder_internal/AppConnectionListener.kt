package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_internal.state.ActivityState
import kotlinx.coroutines.flow.StateFlow

interface AppConnectionListener {
    fun collectState(): StateFlow<ActivityState>
}