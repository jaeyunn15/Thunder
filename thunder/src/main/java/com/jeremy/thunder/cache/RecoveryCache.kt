package com.jeremy.thunder.cache

import com.jeremy.thunder.state.ThunderRequest


/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * 가장 마지막 요청을 캐싱하기 위한 목적의 캐시
 * */

class RecoveryCache {

    private var latestRequest: ThunderRequest? = null

    fun set(value: ThunderRequest) {
        latestRequest = value
    }

    fun get(): ThunderRequest? {
        return latestRequest
    }

    fun hasCache(): Boolean = latestRequest != null

    fun clear() {
        latestRequest = null
    }

    class Factory {

        fun create(): RecoveryCache {
            return RecoveryCache()
        }
    }
}