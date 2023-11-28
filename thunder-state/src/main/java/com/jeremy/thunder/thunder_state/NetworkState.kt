package com.jeremy.thunder.thunder_state

sealed interface NetworkState {
    object Available : NetworkState
    object Unavailable : NetworkState
}