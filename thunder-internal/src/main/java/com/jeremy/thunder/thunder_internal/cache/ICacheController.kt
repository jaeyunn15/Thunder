package com.jeremy.thunder.thunder_internal.cache

interface ICacheController<T> {
    fun getRecovery(): BaseRecovery<T>
    fun getValve(): BaseValve<T>
}