package com.jeremy.thunder.connection

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.jeremy.thunder.thunder_internal.state.ActivityState
import com.jeremy.thunder.thunder_internal.state.Background
import com.jeremy.thunder.thunder_internal.state.Foreground
import com.jeremy.thunder.thunder_internal.state.Initialize
import com.jeremy.thunder.thunder_internal.state.ShutDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppConnectionProvider : com.jeremy.thunder.thunder_internal.AppConnectionListener, Application.ActivityLifecycleCallbacks {

    private val _eventFlow = MutableStateFlow<ActivityState>(Initialize)

    override fun collectState(): StateFlow<ActivityState> {
        return _eventFlow.asStateFlow()
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        _eventFlow.tryEmit(Initialize)
    }

    override fun onActivityStarted(p0: Activity) {
        _eventFlow.tryEmit(Foreground)
    }

    override fun onActivityResumed(p0: Activity) {
        _eventFlow.tryEmit(Foreground)
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        _eventFlow.tryEmit(Background)
    }

    override fun onActivityDestroyed(p0: Activity) {
        _eventFlow.tryEmit(ShutDown)
    }
}