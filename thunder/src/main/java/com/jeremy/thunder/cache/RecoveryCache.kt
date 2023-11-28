package com.jeremy.thunder.cache

import com.jeremy.thunder.thunder_internal.cache.BaseRecovery
import com.jeremy.thunder.thunder_internal.event.ThunderRequest


/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * */

class RecoveryCache: BaseRecovery<ThunderRequest> {

    private var latestRequest: ThunderRequest? = null

    override fun set(value: ThunderRequest) {
        latestRequest = value
    }

    override fun get(): ThunderRequest? {
        return latestRequest
    }

    override fun hasCache(): Boolean = latestRequest != null

    override fun clear() {
        latestRequest = null
    }

    class Factory: BaseRecovery.BaseRecoveryFactory<ThunderRequest> {

        override fun create(): RecoveryCache {
            return RecoveryCache()
        }
    }
}