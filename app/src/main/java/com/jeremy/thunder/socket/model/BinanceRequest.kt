package com.jeremy.thunder.socket.model

data class BinanceRequest(
    val id: Int = 1,
    val method: String = "SUBSCRIBE",
    val params: List<String> = listOf("btcusdt@markPrice")
)

//val params: List<String> = listOf("btcusdt@markPrice", "ethusdt@markPrice")

//[{"ticket":"test"},{"type":"trade","codes":["KRW-BTC","BTC-BCH"]},{"format":"SIMPLE"}]
sealed interface UpbitRequest

data class RequestTicketField(
    val ticket: String
): UpbitRequest

data class RequestTypeField(
    val type: String,
    val codes: List<String>
): UpbitRequest

data class RequestFormatField(
    val format: String = "DEFAULT"
): UpbitRequest