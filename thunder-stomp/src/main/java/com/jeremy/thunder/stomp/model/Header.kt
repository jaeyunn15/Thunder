package com.jeremy.thunder.stomp.model

const val VERSION = "accept-version"
const val HEARTBEAT = "heart-beat"
const val DESTINATION = "destination"
const val CONTENT_TYPE = "content-type"
const val MESSAGE_ID = "message-id"
const val RECEIPT = "receipt"
const val ID = "id"
const val ACK = "ack"
const val DEFAULT_ACK = "auto"
const val SUPPORTED_VERSIONS = "1.1,1.2"

typealias HeaderMap = Pair<String, String>

data class Header(
    val pairList: List<HeaderMap>
) {
    fun extract(): String = pairList.joinToString(separator = "") {
        it.mapValue()
    }
}

fun Pair<String, String>.mapValue() = "$first:$second\n"