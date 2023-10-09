package com.jeremy.thunder.event.converter

sealed class ConverterType {
    object Gson: ConverterType()

    object Serialization: ConverterType()
}