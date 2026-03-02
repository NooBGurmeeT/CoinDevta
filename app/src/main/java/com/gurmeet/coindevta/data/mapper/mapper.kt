package com.gurmeet.coindevta.data.mapper

import com.gurmeet.coindevta.data.local.entity.CoinEntity
import com.gurmeet.coindevta.data.remote.dto.CoinDto
import com.gurmeet.coindevta.domain.model.Coin


fun CoinDto.toEntity() = CoinEntity(
    symbol = symbol,
    name = symbol.replace("USDT", ""),
    price = lastPrice.toDouble(),
    marketCap = quoteVolume.toDoubleOrNull() ?: 0.0,
    changePercent24h = priceChangePercent.toDoubleOrNull() ?: 0.0
)

fun CoinEntity.toDomain() = Coin(
    symbol = symbol,
    name = name,
    price = price,
    marketCap = marketCap,
    changePercent24h = changePercent24h,
    isFavorite = isFavorite,
    isPinned = isPinned
)