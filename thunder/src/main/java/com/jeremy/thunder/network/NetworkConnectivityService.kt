package com.jeremy.thunder.network

import com.jeremy.thunder.NetworkState
import kotlinx.coroutines.flow.Flow

interface NetworkConnectivityService {
    val networkStatus: Flow<NetworkState>
}