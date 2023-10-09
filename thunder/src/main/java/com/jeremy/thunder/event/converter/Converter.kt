package com.jeremy.thunder.event.converter

interface Converter<T> {
    fun convert(data: String): T
}