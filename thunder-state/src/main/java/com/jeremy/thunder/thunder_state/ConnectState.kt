package com.jeremy.thunder.thunder_state

/**
 * */
sealed class ConnectState {
    object Initialize : ConnectState()

    object Establish : ConnectState()

    data class ConnectError(
        val typeOfError: ThunderError
    ) : ConnectState()

    data class ConnectClose(
        val reason: String? = null
    ) : ConnectState()
}

