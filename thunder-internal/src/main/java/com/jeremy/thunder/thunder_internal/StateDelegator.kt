package com.jeremy.thunder.thunder_internal

import com.jeremy.thunder.thunder_state.Active
import com.jeremy.thunder.thunder_state.AppState
import com.jeremy.thunder.thunder_state.ConnectState
import com.jeremy.thunder.thunder_state.Inactive
import com.jeremy.thunder.thunder_state.NetworkState
import com.jeremy.thunder.thunder_state.WebSocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlin.reflect.KProperty

fun stateDelegate(
    applicationStateFlow: Flow<AppState>,
    networkStateFlow: Flow<NetworkState>,
    webSocketEventFlow: Flow<WebSocketEvent>,
    scope: CoroutineScope,
    recovery: () -> Unit
) = StateDelegator(
    applicationStateFlow,
    networkStateFlow,
    webSocketEventFlow,
    scope,
    recovery
)

class StateDelegator(
    applicationStateFlow: Flow<AppState>,
    networkStateFlow: Flow<NetworkState>,
    webSocketEventFlow: Flow<WebSocketEvent>,
    scope: CoroutineScope,
    recovery: () -> Unit
) {

    init {
        applicationStateFlow // Application State (Active is Foreground & Background, Inactive is Destroy)
            .mapLatest { appState ->
                appState.checkAndRecovery {
                    recovery.invoke()
                }
            }
            .combine(networkStateFlow) { upStreamState, networkState -> // Network State (either wifi, mobile)
                if (upStreamState) {
                    networkState.checkAndRecovery {
                        recovery.invoke()
                    }
                } else false
            }.combine(webSocketEventFlow) { upStreamState, webSocketEvent -> // WebSocket Event flow
                if (upStreamState) {
                    when (webSocketEvent) {
                        is WebSocketEvent.OnMessageReceived,
                        is WebSocketEvent.OnConnectionOpen -> {
                            _connectState.update { ConnectState.Establish }
                        }

                        WebSocketEvent.OnConnectionClosed -> {
                            _connectState.update { ConnectState.ConnectClose() }
                        }

                        is WebSocketEvent.OnConnectionError -> {
                            _connectState.update {
                                ConnectState.ConnectError(
                                    com.jeremy.thunder.thunder_state.ThunderError.SocketLoss(
                                        webSocketEvent.error
                                    )
                                )
                            }
                            recovery.invoke()
                        }
                    }
                }
            }.launchIn(scope)
    }

    private fun AppState.checkAndRecovery(recovery: () -> Unit): Boolean {
        return when (this) {
            Active -> true
            Inactive -> {
                recovery.invoke()
                false
            }
        }
    }

    private fun NetworkState.checkAndRecovery(recovery: () -> Unit): Boolean {
        return when (this) {
            NetworkState.Available -> true
            NetworkState.Unavailable -> {
                recovery.invoke()
                false
            }
        }
    }

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): Flow<ConnectState> = _connectState

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Initialize)

}