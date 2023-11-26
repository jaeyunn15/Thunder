package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_internal.state.NetworkState
import kotlinx.coroutines.flow.Flow

interface NetworkConnectivityService {
    val networkStatus: Flow<NetworkState>
    fun hasAvailableNetworks(): Boolean
}