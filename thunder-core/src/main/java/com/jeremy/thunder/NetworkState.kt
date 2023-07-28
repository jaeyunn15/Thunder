package com.jeremy.thunder

sealed interface NetworkState {
    object Available : NetworkState
    object Unavailable : NetworkState
}