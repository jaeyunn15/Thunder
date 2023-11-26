package com.jeremy.thunder.event.converter

import com.jeremy.thunder.thunderLog
import com.jeremy.thunder.thunder_internal.Converter
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.internal.ignoreIoExceptions
import java.lang.reflect.Type

class SerializeConvertAdapter <T> private constructor(
    type: Type,
    private val kSerializer: KSerializer<T>
) : Converter<T> {
    private fun SerialFormat.serializeAsType(type: Type): KSerializer<Any> = serializersModule.serializer(type)
    class Factory {
        fun create(type: Type): SerializeConvertAdapter<*> {
            return SerializeConvertAdapter(type, serializer(type))
        }
    }

    private val json = Json {
        isLenient = true
        useAlternativeNames = true // can use alternative key
        ignoreUnknownKeys = true // prevent ignore no matching key exception
        coerceInputValues = true // Set as non-null type but receive as null type. you can use default value as set true
        encodeDefaults = true // You can use default value when received data does not have that value.
    }

    private val stringFormat: StringFormat = json.apply {
        ignoreIoExceptions {
            thunderLog("[IoException] Cause convert specific type is not supported.")
        }
    }

    private val loader: DeserializationStrategy<T> = stringFormat.serializeAsType(type) as DeserializationStrategy<T>

    override fun convert(data: String): T {
        return kotlin.run {
            stringFormat.decodeFromString(loader, data)
        }
    }
}
