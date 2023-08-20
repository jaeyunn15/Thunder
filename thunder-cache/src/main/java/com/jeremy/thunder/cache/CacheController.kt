package com.jeremy.thunder.cache

import kotlinx.coroutines.CoroutineScope

class CacheController(
    private val recoveryCache: RecoveryCache,
    private val valveCache: ValveCache
) {

    val rCache: RecoveryCache get() = recoveryCache

    val vCache: ValveCache get() = valveCache


    class Factory(
        private val scope: CoroutineScope
    ) {
        private var needValveCache: Boolean = false

        fun setValveCache(valveCacheNeed: Boolean) = apply {
            needValveCache = valveCacheNeed
        }

        private fun createRecoveryCache(): RecoveryCache {
            return RecoveryCache.Factory().create()
        }

        private fun createValveCache(): ValveCache {
            return ValveCache.Factory(scope).create()
        }

        fun create(): CacheController {
            return CacheController(
                createRecoveryCache(),
                createValveCache()
            )
        }
    }
}