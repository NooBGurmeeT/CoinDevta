package com.gurmeet.coindevta.data.remote.dto


data class CoinDto(
    val symbol: String,
    val lastPrice: String,
    val quoteVolume: String,          // market cap proxy
    val priceChangePercent: String    // 24h %
)