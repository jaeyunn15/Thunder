package com.jeremy.thunder.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.jeremy.thunder.thunder_internal.state.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn


class NetworkConnectivityServiceImpl constructor (
    context: Context
): com.jeremy.thunder.thunder_internal.NetworkConnectivityService {

    private val networks = HashSet<Long>()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun hasAvailableNetworks(): Boolean = networks.isNotEmpty()

    override val networkStatus: Flow<NetworkState> = callbackFlow {
        val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networks.add(network.networkHandle)
                if (networks.isNotEmpty()) {
                    trySend(NetworkState.Available)
                }
            }

            override fun onUnavailable() {
                trySend(NetworkState.Unavailable)
            }

            override fun onLost(network: Network) {
                networks.remove(network.networkHandle)
                if (networks.isEmpty()) {
                    trySend(NetworkState.Unavailable)
                }
            }

        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(request, connectivityCallback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

}