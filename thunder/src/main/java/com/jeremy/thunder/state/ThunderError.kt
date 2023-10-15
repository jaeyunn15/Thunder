package com.jeremy.thunder.state

sealed interface ThunderError {
    object General : ThunderError

    object NetworkLoss : ThunderError

    data class SocketLoss(
        val message: String?
    ) : ThunderError
}

