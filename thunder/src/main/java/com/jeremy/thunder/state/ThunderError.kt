package com.jeremy.thunder.state

sealed interface ThunderError {
    data class NetworkLoss(
        val message: String?
    ) : ThunderError

    data class SocketLoss(
        val message: String?
    ) : ThunderError

    data class Else(
        val message: String?
    ) : ThunderError
}

