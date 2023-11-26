package com.jeremy.thunder.thunder_internal.state

sealed interface ThunderError {
    object General : ThunderError

    object NetworkLoss : ThunderError

    data class SocketLoss(
        val message: String?
    ) : ThunderError
}

