package com.jeremy.thunder.cache


/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * 가장 마지막 요청을 캐싱하기 위한 목적의 캐시
 * */

class RecoveryCache {

    private var latestRequest: String? = null

    fun set(value: String) {
        latestRequest = value
    }

    fun get(): String? {
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