package com.jeremy.thunder

sealed interface ThunderState {
    object IDLE : ThunderState

    object CONNECTING : ThunderState

    object CONNECTED : ThunderState

    object DISCONNECTING : ThunderState

    object DISCONNECTED : ThunderState

    data class ERROR(
        val error: ThunderError
    ) : ThunderState
}