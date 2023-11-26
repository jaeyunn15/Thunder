package com.jeremy.thunder.cache

import com.jeremy.thunder.thunder_internal.BaseRecovery
import com.jeremy.thunder.thunder_internal.BaseValve
import com.jeremy.thunder.thunder_internal.ICacheController
import com.jeremy.thunder.thunder_internal.event.ThunderRequest

/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * */

class CacheController (
    private val recoveryCache: BaseRecovery<ThunderRequest>,
    private val valveCache: BaseValve<ThunderRequest>
): ICacheController<ThunderRequest> {

    override fun getRecovery(): BaseRecovery<ThunderRequest> {
        return recoveryCache
    }

    override fun getValve(): BaseValve<ThunderRequest> {
        return valveCache
    }


    class Factory() {
        private fun createRecoveryCache(): RecoveryCache {
            return RecoveryCache.Factory().create()
        }

        private fun createValveCache(): ValveCache {
            return ValveCache.Factory().create()
        }

        fun create(): CacheController {
            return CacheController(
                createRecoveryCache(),
                createValveCache()
            )
        }
    }
}