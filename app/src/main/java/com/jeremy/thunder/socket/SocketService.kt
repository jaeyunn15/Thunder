package com.jeremy.thunder.socket

import com.jeremy.thunder.socket.model.AllMarketResponse
import com.jeremy.thunder.socket.model.AllMarketTickerResponse
import com.jeremy.thunder.socket.model.BinanceRequest
import com.jeremy.thunder.socket.model.TickerResponse
import com.jeremy.thunder.socket.model.UpbitRequest
import com.jeremy.thunder.socket.model.UpbitTickerResponse
import com.jeremy.thunder.ws.Receive
import com.jeremy.thunder.ws.Send
import kotlinx.coroutines.flow.Flow

interface SocketService {
    @Send
    fun request(request: BinanceRequest)

    @Send
    fun requestUpbit(request: List<UpbitRequest>)

    @Receive
    fun collectUpbitTicker(): Flow<UpbitTickerResponse>


    @Receive
    fun observeTicker(): Flow<TickerResponse>

    @Receive
    fun observeAllMarkets(): Flow<AllMarketResponse>

    @Receive
    fun observeAllMarketTickers(): Flow<AllMarketTickerResponse>
}