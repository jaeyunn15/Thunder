package com.jeremy.thunder.thunder_internal.state

sealed interface ActivityState

object Initialize: ActivityState

object Foreground: ActivityState

object Background: ActivityState

object ShutDown: ActivityState