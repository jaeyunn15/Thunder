package com.jeremy.thunder.connection

import com.jeremy.thunder.state.ActivityState
import kotlinx.coroutines.flow.StateFlow

interface AppConnectionListener {
    fun collectState(): StateFlow<ActivityState>
}