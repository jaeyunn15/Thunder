package com.jeremy.thunder.thunder_internal.state

sealed interface ManagerState
object ThunderManager : ManagerState
object StompManager : ManagerState