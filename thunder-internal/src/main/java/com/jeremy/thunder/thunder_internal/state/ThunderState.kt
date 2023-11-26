package com.jeremy.thunder.thunder_internal.state

/**
 * Socket Thunder State
 * @see [com.jeremy.thunder.internal.ThunderStateManager]
* */
sealed interface ThunderState {

    /**
     * Initial Socket State
     * */
    object IDLE : ThunderState

    /**
     * Ready for connect.
    * */
    object CONNECTING : ThunderState

    /**
     * Connection Available and can send data or receive payload.
     * */
    object CONNECTED : ThunderState

    /**
     * Ready for connection close.
    * */
    object DISCONNECTING : ThunderState

    /**
     * Close Connection.
    * */
    object DISCONNECTED : ThunderState

    /**
     * Error State and [ThunderError] must be defined.
    * */
    data class ERROR(
        val error: ThunderError = ThunderError.General
    ) : ThunderState
}