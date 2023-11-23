package com.jeremy.thunder.state

sealed interface ManagerState
object ThunderManager : ManagerState
object StompManager : ManagerState