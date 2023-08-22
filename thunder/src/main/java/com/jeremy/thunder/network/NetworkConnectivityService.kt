package com.jeremy.thunder.network

import kotlinx.coroutines.flow.Flow

interface NetworkConnectivityService {
    val networkStatus: Flow<com.jeremy.thunder.core.NetworkState>
}