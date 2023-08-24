package com.jeremy.thunder.socket.model

import com.google.gson.annotations.SerializedName

/*
* sample response
* {"stream":"btcusdt@markPrice","data":{"e":"markPriceUpdate","E":1690708629000,"s":"BTCUSDT","p":"29273.20000000","P":"29298.59899942","i":"29287.09142857","r":"0.00006290","T":1690732800000}}
* */

data class TickerResponse(
    @SerializedName("stream")
    val stream: String,

    @SerializedName("data")
    val data: Ticker
)

/*
* {
    "e": "markPriceUpdate",     // Event type
    "E": 1562305380000,         // Event time
    "s": "BTCUSDT",             // Symbol
    "p": "11794.15000000",      // Mark price
    "i": "11784.62659091",      // Index price
    "P": "11784.25641265",      // Estimated Settle Price, only useful in the last hour before the settlement starts
    "r": "0.00038167",          // Funding rate
    "T": 1562306400000          // Next funding time
  }
* */

data class Ticker(
    @SerializedName("E")
    val E: Long, // Event time

    @SerializedName("T")
    val T: Long,  // Next funding time

    @SerializedName("i")
    val i: String,  // Index price

    @SerializedName("p")
    val p: String, // Mark price

    @SerializedName("r")
    val r: String, // Funding rate

    @SerializedName("s")
    val s: String // Symbol
)
