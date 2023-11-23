package com.jeremy.thunder.stomp.compiler

sealed interface ThunderStompResponse {
    val command: ResponseCommandType
}

enum class ResponseCommandType {
    MESSAGE,
    RECEIPT,
    ERROR,
    UNIT
}

object UnitResponse: ThunderStompResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.UNIT
}

data class MessageResponse(
    val header: HashMap<String, String>,
    val payload: String?
): ThunderStompResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.MESSAGE
}

data class ReceiptResponse(
    val header: HashMap<String, String>,
): ThunderStompResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.RECEIPT
}

data class ErrorResponse(
    val payload: String?
): ThunderStompResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.ERROR
}
