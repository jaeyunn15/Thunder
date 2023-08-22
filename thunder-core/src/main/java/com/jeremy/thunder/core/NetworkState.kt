package com.jeremy.thunder.core

sealed interface NetworkState {
    object Available : NetworkState
    object Unavailable : NetworkState
}