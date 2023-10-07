package com.jeremy.thunder.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object CoroutineScope {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}