package com.gurmeet.coindevta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val price: Double,
    val marketCap: Double,
    val changePercent24h: Double,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
)