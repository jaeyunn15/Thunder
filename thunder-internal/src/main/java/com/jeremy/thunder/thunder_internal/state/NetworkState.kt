package com.jeremy.thunder.thunder_internal.state

sealed interface NetworkState {
    object Available : NetworkState
    object Unavailable : NetworkState
}