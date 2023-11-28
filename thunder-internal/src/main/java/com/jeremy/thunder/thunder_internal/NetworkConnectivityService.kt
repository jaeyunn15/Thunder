package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_state.NetworkState
import kotlinx.coroutines.flow.Flow

interface NetworkConnectivityService {
    val networkStatus: Flow<com.jeremy.thunder.thunder_state.NetworkState>
    fun hasAvailableNetworks(): Boolean
}