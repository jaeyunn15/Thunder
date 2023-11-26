package com.jeremy.thunder.thunder_internal

interface BaseRecovery<T> {
    fun set(value: T)
    fun get(): T?

    fun hasCache(): Boolean
    fun clear()

    interface BaseRecoveryFactory<T> {
        fun create(): BaseRecovery<T>
    }
}