package com.jeremy.thunder.thunder_internal

interface Converter<T> {
    fun convert(data: String): T
}