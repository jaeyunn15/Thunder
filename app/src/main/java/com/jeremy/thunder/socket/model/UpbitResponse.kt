package com.jeremy.thunder.socket.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class UpbitTickerResponse(
    @SerializedName("acc_ask_volume")
    val acc_ask_volume: Double = 0.0,

    @SerializedName("acc_bid_volume")
    val acc_bid_volume: Double = 0.0,

    @SerializedName("acc_trade_price")
    val acc_trade_price: Double = 0.0,

    @SerializedName("acc_trade_price_24h")
    val acc_trade_price_24h: Double = 0.0,

    @SerializedName("acc_trade_volume")
    val acc_trade_volume: Double = 0.0,

    @SerializedName("acc_trade_volume_24h")
    val acc_trade_volume_24h: Double = 0.0,

    @SerializedName("ask_bid")
    val ask_bid: String = "",

    @SerializedName("change")
    val change: String = "",

    @SerializedName("change_price")
    val change_price: Double = 0.0,

    @SerializedName("change_rate")
    val change_rate: Double = 0.0,

    @SerializedName("code")
    val code: String = "",

    @SerializedName("high_price")
    val high_price: Double = 0.0,

    @SerializedName("highest_52_week_date")
    val highest_52_week_date: String = "",

    @SerializedName("highest_52_week_price")
    val highest_52_week_price: Double = 0.0,

    @SerializedName("is_trading_suspended")
    val is_trading_suspended: Boolean = false, //

    @SerializedName("low_price")
    val low_price: Double = 0.0,

    @SerializedName("lowest_52_week_date")
    val lowest_52_week_date: String = "",

    @SerializedName("lowest_52_week_price")
    val lowest_52_week_price: Double = 0.0,

    @SerializedName("market_state")
    val market_state: String = "", //

    @SerializedName("market_warning")
    val market_warning: String = "", //

    @SerializedName("opening_price")
    val opening_price: Double = 0.0,

    @SerializedName("prev_closing_price")
    val prev_closing_price: Double = 0.0,

    @SerializedName("signed_change_price")
    val signed_change_price: Double = 0.0,

    @SerializedName("signed_change_rate")
    val signed_change_rate: Double = 0.0,

    @SerializedName("stream_type")
    val stream_type: String = "",

    @SerializedName("timestamp")
    val timestamp: Long = 0L,

    @SerializedName("trade_date")
    val trade_date: String = "",

    @SerializedName("trade_price")
    val trade_price: Double = 0.0,

    @SerializedName("trade_time")
    val trade_time: String = "",

    @SerializedName("trade_timestamp")
    val trade_timestamp: Long = 0L,

    @SerializedName("trade_volume")
    val trade_volume: Double = 0.0,

    @SerializedName("type")
    val type: String = ""
)