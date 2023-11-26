package com.jeremy.thunder.event.converter

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.jeremy.thunder.thunder_internal.Converter
import java.io.StringReader
import java.lang.reflect.Type

class GsonConvertAdapter<T> private constructor(
    private val gson: Gson,
    private val typeAdapter: TypeAdapter<T>,
    private val type: Type
) : Converter<T> {

    override fun convert(data: String): T {
        val jsonReader = gson.newJsonReader(StringReader(data))
        return typeAdapter.read(jsonReader)!!
    }

    class Factory {
        fun create(type: Type): GsonConvertAdapter<*> {
            val typeAdapter = Gson().getAdapter(TypeToken.get(type))
            return GsonConvertAdapter(Gson(), typeAdapter, type)
        }
    }
}