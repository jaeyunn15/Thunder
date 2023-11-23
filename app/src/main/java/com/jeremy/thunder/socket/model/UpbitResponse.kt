package com.jeremy.thunder.socket.model

import kotlinx.serialization.Serializable

@Serializable
data class UpbitTickerResponse(
    val acc_ask_volume: Double = 0.0,
    val acc_bid_volume: Double = 0.0,
    val acc_trade_price: Double = 0.0,
    val acc_trade_price_24h: Double = 0.0,
    val acc_trade_volume: Double = 0.0,
    val acc_trade_volume_24h: Double = 0.0,
    val ask_bid: String = "",
    val change: String = "",
    val change_price: Double = 0.0,
    val change_rate: Double = 0.0,
    val code: String = "",
    val high_price: Double = 0.0,
    val highest_52_week_date: String = "",
    val highest_52_week_price: Double = 0.0,
    val is_trading_suspended: Boolean = false, //
    val low_price: Double = 0.0,
    val lowest_52_week_date: String = "",
    val lowest_52_week_price: Double = 0.0,
    val market_state: String = "", //
    val market_warning: String = "", //
    val opening_price: Double = 0.0,
    val prev_closing_price: Double = 0.0,
    val signed_change_price: Double = 0.0,
    val signed_change_rate: Double = 0.0,
    val stream_type: String = "",
    val timestamp: Long = 0L,
    val trade_date: String = "",
    val trade_price: Double = 0.0,
    val trade_time: String = "",
    val trade_timestamp: Long = 0L,
    val trade_volume: Double = 0.0,
    val type: String = ""
)