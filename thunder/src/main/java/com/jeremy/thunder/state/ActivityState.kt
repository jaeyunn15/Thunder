package com.jeremy.thunder.state

sealed interface ActivityState

object Initialize: ActivityState

object Foreground: ActivityState

object Background: ActivityState

object ShutDown: ActivityState