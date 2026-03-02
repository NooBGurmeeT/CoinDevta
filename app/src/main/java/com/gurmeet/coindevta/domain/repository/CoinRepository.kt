package com.gurmeet.coindevta.domain.repository

import com.gurmeet.coindevta.domain.model.Coin
import com.gurmeet.coindevta.util.Response
import kotlinx.coroutines.flow.Flow

interface CoinRepository {

    suspend fun loadInitialData(): Response<Unit>

    fun observeCoins(): Flow<List<Coin>>

    // 🔥 Clean streaming API (no Response wrapper)
    fun observeLivePrices(): Flow<Pair<String, Double>>

    suspend fun syncPrices(): Response<Unit>

    suspend fun toggleFavorite(symbol: String)

    suspend fun pinCoin(symbol: String)

    suspend fun unPinCoin()

    suspend fun getPinnedCoin(): Coin?
}