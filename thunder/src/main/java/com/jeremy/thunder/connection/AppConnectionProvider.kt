package com.jeremy.thunder.connection

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.jeremy.thunder.state.ActivityState
import com.jeremy.thunder.state.GetReady
import com.jeremy.thunder.state.Initialize
import com.jeremy.thunder.state.ShutDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppConnectionProvider : AppConnectionListener, Application.ActivityLifecycleCallbacks {

    private val _eventFlow = MutableStateFlow<ActivityState>(Initialize)

    override fun collectState(): StateFlow<ActivityState> {
        return _eventFlow.asStateFlow()
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        _eventFlow.tryEmit(GetReady)
    }

    override fun onActivityStarted(p0: Activity) {
        _eventFlow.tryEmit(GetReady)
    }

    override fun onActivityResumed(p0: Activity) {
        _eventFlow.tryEmit(GetReady)
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityDestroyed(p0: Activity) {
        _eventFlow.tryEmit(ShutDown)
    }
}