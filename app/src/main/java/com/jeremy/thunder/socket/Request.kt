package com.jeremy.thunder.socket

import com.google.gson.annotations.SerializedName
import java.util.UUID

sealed interface Request

data class Ticket(
    @SerializedName("ticket")
    val ticket: String = UUID.randomUUID().toString(),
): Request

data class Type (
    @SerializedName("codes")
    val codes: List<String> = listOf("KRW-BTC", "KRW-ETH"), //KRW-BTC

    @SerializedName("type")
    val type: String = "ticker"
): Request