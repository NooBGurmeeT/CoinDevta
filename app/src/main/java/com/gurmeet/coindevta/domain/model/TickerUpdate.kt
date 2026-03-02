package com.gurmeet.coindevta.domain.model

data class TickerUpdate(
    val symbol: String,
    val currentPrice: Double,
    val openPrice24h: Double,
    val isPositive24h: Boolean,
)