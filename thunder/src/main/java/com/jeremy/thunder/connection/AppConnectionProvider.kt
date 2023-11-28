package com.jeremy.thunder.connection

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.jeremy.thunder.thunder_internal.AppConnectionListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AppConnectionProvider : AppConnectionListener, Application.ActivityLifecycleCallbacks {

    private val _eventFlow = MutableStateFlow<com.jeremy.thunder.thunder_state.AppState>(com.jeremy.thunder.thunder_state.Active)

    override fun collectAppState(): Flow<com.jeremy.thunder.thunder_state.AppState> {
        return _eventFlow
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        _eventFlow.tryEmit(com.jeremy.thunder.thunder_state.Active)
    }

    override fun onActivityStarted(p0: Activity) {
        _eventFlow.tryEmit(com.jeremy.thunder.thunder_state.Active)
    }

    override fun onActivityResumed(p0: Activity) {
        _eventFlow.tryEmit(com.jeremy.thunder.thunder_state.Active)
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityDestroyed(p0: Activity) {
        _eventFlow.tryEmit(com.jeremy.thunder.thunder_state.Inactive)
    }
}