package com.jeremy.thunder.stomp.compiler

import com.jeremy.thunder.stomp.model.Command
import com.jeremy.thunder.stomp.model.Header
import com.jeremy.thunder.stomp.model.HeaderMap

class ThunderRequest private constructor(
    val command: Command,
    val header: Header,
    val payload: String?
) {

    class Builder {
        private var command: Command? = null
        private var header: Header? = null
        private var payload: String? = null
        private val list = mutableListOf<HeaderMap>()

        fun command(value: Command) = apply { command = value }

        fun header(vararg pair: HeaderMap) = apply {
            pair.map(list::add)
            header = Header(list)
        }

        fun payload(value: String) = apply { payload = value }

        fun build(): ThunderRequest {
            requireNotNull(command) { "Command must be set" }
            requireNotNull(header) { "Header must be set" }

            return ThunderRequest(command!!, header!!, payload)
        }
    }
}
