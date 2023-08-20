package com.jeremy.thunder.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * 가장 마지막 요청을 캐싱하기 위한 목적의 캐시
 * */

class RecoveryCache {

    private val requestCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    fun set(key: String, value: String) {
        requestCache[key] = value
    }

    fun set(value: String) {
        requestCache[value] =value
    }

    fun get(): List<String> {
        return requestCache.map { it.value }
    }

    fun hasCache(): Boolean = requestCache.isNotEmpty()

    fun clear() {
        requestCache.clear()
    }

    class Factory {

        fun create(): RecoveryCache {
            return RecoveryCache()
        }
    }
}