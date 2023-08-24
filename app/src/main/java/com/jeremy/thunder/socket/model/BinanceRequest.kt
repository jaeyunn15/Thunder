package com.jeremy.thunder.socket.model

data class BinanceRequest(
    val id: Int = 1,
    val method: String = "SUBSCRIBE",
    val params: List<String> = listOf("btcusdt@markPrice")
)

//val params: List<String> = listOf("btcusdt@markPrice", "ethusdt@markPrice")