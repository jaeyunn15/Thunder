package com.jeremy.thunder.thunder_internal.event

import com.google.gson.annotations.SerializedName

interface ThunderRequest

data class WebSocketRequest(
    val msg: String
) : ThunderRequest


data class StompRequest(
    @SerializedName("command")
    val command: String,

    @SerializedName("destination")
    val destination: String,

    @SerializedName("payload")
    val payload: String? = "",
) : ThunderRequest