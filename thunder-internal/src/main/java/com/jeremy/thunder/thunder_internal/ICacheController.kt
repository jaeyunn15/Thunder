package com.jeremy.thunder.thunder_internal

interface ICacheController<T> {
    fun getRecovery(): BaseRecovery<T>
    fun getValve(): BaseValve<T>
}