package com.jeremy.thunder.event

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import java.io.StringReader
import java.lang.reflect.Type

interface Converter<T> {
    fun convert(data: String): T
}

class ConvertAdapter<T> private constructor(
    private val gson: Gson,
    private val typeAdapter: TypeAdapter<T>,
    private val type: Type
) : Converter<T> {

    override fun convert(data: String): T {
        val jsonReader = gson.newJsonReader(StringReader(data))
        return typeAdapter.read(jsonReader)!!
    }

    class Factory {
        fun create(type: Type): ConvertAdapter<*> {
            val typeAdapter = Gson().getAdapter(TypeToken.get(type))
            return ConvertAdapter(Gson(), typeAdapter, type)
        }
    }
}