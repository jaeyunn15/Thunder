package com.jeremy.thunder.state

sealed interface ActivityState

object Initialize: ActivityState

object GetReady: ActivityState

object ShutDown: ActivityState