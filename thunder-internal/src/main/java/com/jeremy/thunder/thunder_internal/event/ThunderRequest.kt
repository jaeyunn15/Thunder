package com.jeremy.thunder.thunder_internal.event

import com.google.gson.annotations.SerializedName

enum class RequestType {
    GENERAL, // for general websocket
    STOMP_SUBSCRIBE, // for stomp subscribe
    STOMP_SEND, // for stomp send
}

sealed interface ThunderRequest {
    val typeOfRequest: RequestType
}

data class WebSocketRequest(
    val msg: String
) : ThunderRequest {
    override val typeOfRequest: RequestType = RequestType.GENERAL
}

data class StompSendRequest(
    @SerializedName("command")
    val command: String,

    @SerializedName("destination")
    val destination: String,

    @SerializedName("payload")
    val payload: String? = "",
) : ThunderRequest {
    override val typeOfRequest: RequestType = RequestType.STOMP_SEND
}

data class StompSubscribeRequest(
    @SerializedName("subscribe")
    val subscribe: Boolean ,

    @SerializedName("destination")
    val destination: String,

    @SerializedName("payload")
    val payload: String? = "",
) : ThunderRequest {
    override val typeOfRequest: RequestType = RequestType.STOMP_SUBSCRIBE
}