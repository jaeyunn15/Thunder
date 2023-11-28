package com.jeremy.thunder.thunder_state

sealed interface ManagerState
object ThunderManager : ManagerState
object StompManager : ManagerState