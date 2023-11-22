package com.jeremy.thunder.socket.model

import kotlinx.serialization.Serializable

@Serializable
data class UpbitTickerResponse(
    val acc_trade_price: Double,
    val acc_trade_price_24h: Double,
    val acc_trade_volume: Double,
    val acc_trade_volume_24h: Double,
    val change: String,
    val change_price: Double,
    val change_rate: Double,
    val code: String,
    val high_price: Double,
    val highest_52_week_date: String,
    val highest_52_week_price: Double,
    val is_trading_suspended: Boolean, //
    val low_price: Double,
    val lowest_52_week_date: String,
    val lowest_52_week_price: Double,
    val market_state: String, //
    val market_warning: String, //
    val opening_price: Double,
    val prev_closing_price: Double,
    val signed_change_price: Double,
    val signed_change_rate: Double,
    val stream_type: String,
    val timestamp: Long,
    val trade_date: String,
    val trade_price: Double,
    val trade_time: String,
    val trade_timestamp: Long,
    val trade_volume: Double,
    val type: String
)