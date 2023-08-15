package com.jeremy.thunder.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * A controller that handles caching for requests from interfaces with the @Send annotation.
 * */

class CacheController {

    private val requestCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    fun set(key: String, value: String) {
        requestCache[key] = value
    }

    fun get(): List<String> {
        return requestCache.map { it.value }
    }

    fun hasCache(): Boolean = requestCache.isNotEmpty()

    fun clear() {
        requestCache.clear()
    }

    class Factory {

        fun create(): CacheController {
            return CacheController()
        }
    }
}