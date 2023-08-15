package com.jeremy.thunder

import java.lang.reflect.Type

/**
 * 특정 어노테이션과 리턴 타입에 대해선 하나의 파이프라인으로 데이터가 통하도록 생성.
 * return type: Flow<TickerResponse> 라면 @Receive + Flow<TickerResponse>를 리턴 타입으로 갖는 메소드는 동일한 EventMapper를 사용하여 데이터를 역직렬화 하게 됨
* */

class SocketEventKeyStore {

    private val cache = mutableMapOf<EventKey, EventMapper<*>>()

    fun findEventMapper(returnType: Type, annotations: Array<Annotation>, converter: Converter<*>): EventMapper<*> {
        val key = EventKey(returnType, annotations)

        if (cache.containsKey(key)) {
            return cache[key]!!
        }

        val eventMapper = EventMapper.Factory().create(converter)

        cache[key] = eventMapper

        return eventMapper
    }

}

data class EventKey(
    val returnType: Type,
    val annotations: Array<Annotation>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventKey

        if (returnType != other.returnType) return false
        if (!annotations.contentEquals(other.annotations)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = returnType.hashCode()
        result = 31 * result + annotations.contentHashCode()
        return result
    }
}