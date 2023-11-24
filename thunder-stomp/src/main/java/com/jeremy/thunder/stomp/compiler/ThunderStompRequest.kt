package com.jeremy.thunder.stomp.compiler

import com.jeremy.thunder.stomp.model.Command
import com.jeremy.thunder.stomp.model.Header
import com.jeremy.thunder.stomp.model.HeaderMap

data class ThunderStompRequest(
    val command: Command,
    val header: Header,
    val payload: String?
)

class ThunderStompHeaderBuilder {
    private var pairList: MutableList<HeaderMap> = mutableListOf()

    infix fun String.to(that: String) {
        pairList.add(Pair(this, that))
    }

    fun build(): Header {
        return Header(
            pairList = pairList
        )
    }
}

class ThunderStompRequestBuilder {
    var command: Command? = null
    var header: Header? = null
    var payload: String? = null

    fun header(init: ThunderStompHeaderBuilder.() -> Unit) {
        header = ThunderStompHeaderBuilder().apply(init).build()
    }
}

fun thunderStompRequest(init: ThunderStompRequestBuilder.() -> Unit): ThunderStompRequest {
    val builder = ThunderStompRequestBuilder()
    builder.init()
    return ThunderStompRequest(
        command = requireNotNull(builder.command) { "Command must be specified" },
        header = requireNotNull(builder.header) { "Header must be specified" },
        payload = builder.payload
    )
}