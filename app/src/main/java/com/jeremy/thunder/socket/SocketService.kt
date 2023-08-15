package com.jeremy.thunder.socket

import com.jeremy.thunder.ws.Receive
import com.jeremy.thunder.ws.Send
import kotlinx.coroutines.flow.Flow

interface SocketService {
    @Send
    fun request(request: BinanceRequest)

    @Receive
    fun observeTicker(): Flow<TickerResponse>

    @Receive
    fun observeAllMarkets(): Flow<AllMarketResponse>
}