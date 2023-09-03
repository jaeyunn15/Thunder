package com.jeremy.thunder.state

sealed interface NetworkState {
    object Available : NetworkState
    object Unavailable : NetworkState
}