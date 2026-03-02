package com.gurmeet.coindevta.domain.model

data class Coin(
    val symbol: String,
    val name: String,
    val price: Double,
    val marketCap: Double,
    val changePercent24h: Double,
    val isFavorite: Boolean,
    val isPinned: Boolean
)