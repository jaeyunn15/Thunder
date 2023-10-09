package com.jeremy.thunder.socket.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AllMarketTickerResponse(
    //@SerializedName("stream")
    val stream: String = "",

    //@SerializedName("data")
    val data: List<AllMarketTickerResponseItem> = emptyList(),

    val nickName: String = "default"
)

/*
*
* [
    {
      "e": "24hrTicker",  // Event type
      "E": 123456789,     // Event time
      "s": "BTCUSDT",     // Symbol
      "p": "0.0015",      // Price change
      "P": "250.00",      // Price change percent
      "w": "0.0018",      // Weighted average price
      "c": "0.0025",      // Last price
      "Q": "10",          // Last quantity
      "o": "0.0010",      // Open price
      "h": "0.0025",      // High price
      "l": "0.0010",      // Low price
      "v": "10000",       // Total traded base asset volume
      "q": "18",          // Total traded quote asset volume
      "O": 0,             // Statistics open time
      "C": 86400000,      // Statistics close time
      "F": 0,             // First trade ID
      "L": 18150,         // Last trade Id
      "n": 18151          // Total number of trades
    }
]
* { "e":"24hrTicker", "E":1692861869895, "s":"BTCUSDT", "p":"389.00", "P":"1.491", "w":"26316.30", "c":"26478.90", "Q":"0.029", "o":"26089.90", "h":"26806.00", "l":"25800.00", "v":"398098.584", "q":"10476480687.75", "O":1692775440000, "C":1692861869890, "F":4030589448, "L":4033831072, "n":3241595 }
*
* */

@Serializable
data class AllMarketTickerResponseItem(
    //@SerializedName("E")
    val E: Long, //event time

    val e: String,

    //@SerializedName("P")
    val P: String, // price change percent

    //@SerializedName("c")
    val c: String, // last Price

    //@SerializedName("h")
    val h: String, // high price

    //@SerializedName("l")
    val l: String, // low price

    //@SerializedName("o")
    val o: String, // open price

//    @SerializedName("s")
    val s: String, // symbol
)
