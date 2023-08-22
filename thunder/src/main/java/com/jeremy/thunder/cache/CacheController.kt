package com.jeremy.thunder.cache

import kotlinx.coroutines.CoroutineScope

/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * */

class CacheController(
    private val recoveryCache: RecoveryCache,
    private val valveCache: ValveCache
) {

    val rCache: RecoveryCache get() = recoveryCache

    val vCache: ValveCache get() = valveCache


    class Factory(
        private val scope: CoroutineScope
    ) {
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