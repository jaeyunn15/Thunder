package com.jeremy.thunder.stomp.compiler

sealed interface ThunderResponse {
    val command: ResponseCommandType
}

enum class ResponseCommandType {
    MESSAGE,
    RECEIPT,
    ERROR,
    UNIT
}

object UnitResponse: ThunderResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.UNIT
}

data class MessageResponse(
    val header: HashMap<String, String>,
    val payload: String?
): ThunderResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.MESSAGE
}

data class ReceiptResponse(
    val header: HashMap<String, String>,
): ThunderResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.RECEIPT
}

data class ErrorResponse(
    val payload: String?
): ThunderResponse {
    override val command: ResponseCommandType
        get() = ResponseCommandType.ERROR
}
