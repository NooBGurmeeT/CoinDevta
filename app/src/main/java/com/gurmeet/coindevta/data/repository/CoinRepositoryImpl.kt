package com.gurmeet.coindevta.data.repository

import com.gurmeet.coindevta.data.local.dao.CoinDao
import com.gurmeet.coindevta.data.mapper.toDomain
import com.gurmeet.coindevta.data.mapper.toEntity
import com.gurmeet.coindevta.data.remote.api.BinanceApi
import com.gurmeet.coindevta.data.remote.websocket.BinanceSocketManager
import com.gurmeet.coindevta.domain.model.Coin
import com.gurmeet.coindevta.domain.model.TickerUpdate
import com.gurmeet.coindevta.domain.repository.CoinRepository
import com.gurmeet.coindevta.util.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class CoinRepositoryImpl @Inject constructor(
    private val api: BinanceApi,
    private val dao: CoinDao,
    private val socketManager: BinanceSocketManager,
) : CoinRepository {

    // ---------------------------------------------------------
    // 1️⃣ Initial Data Load
    // ---------------------------------------------------------

    override suspend fun loadInitialData(): Response<Unit> {
        return try {

            val remoteCoins = api.getAllCoins()
                .filter { it.symbol.endsWith("USDT") }

            val existingCoins = dao.getAllCoinsOnce()
                .associateBy { it.symbol }

            val mergedCoins = remoteCoins.map { dto ->
                val existing = existingCoins[dto.symbol]

                dto.toEntity().copy(
                    isFavorite = existing?.isFavorite ?: false,
                    isPinned = existing?.isPinned ?: false
                )
            }

            dao.insertAll(mergedCoins)

            Response.Success(Unit)

        } catch (e: Exception) {
            Response.Error("Initial load failed", e)
        }
    }

    // ---------------------------------------------------------
    // 2️⃣ Observe Static DB Data
    // ---------------------------------------------------------

    override fun observeCoins(): Flow<List<Coin>> {
        return dao.observeCoins()
            .map { list -> list.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    // ---------------------------------------------------------
    // 3️⃣ Live WebSocket Prices (NO DB WRITE)
    // ---------------------------------------------------------

    override fun observeLivePrices(): Flow<TickerUpdate> {
        return socketManager.observeAllPrices()
            .buffer(capacity = 5000)
            .conflate()
            .flowOn(Dispatchers.IO)
    }

    // ---------------------------------------------------------
    // 4️⃣ Periodic REST Sync (Static Updates Only)
    // ---------------------------------------------------------

    override suspend fun syncPrices(): Response<Unit> {
        return try {

            val coins = api.getAllCoins()
                .filter { it.symbol.endsWith("USDT") }

            coins.forEach {
                dao.updateCoinData(
                    symbol = it.symbol,
                    price = it.lastPrice.toDouble(),
                    marketCap = it.quoteVolume.toDouble(),
                    changePercent24h = it.priceChangePercent.toDouble()
                )
            }

            Response.Success(Unit)

        } catch (e: Exception) {
            Response.Error("Sync failed", e)
        }
    }

    // ---------------------------------------------------------
    // 5️⃣ Favorites / Pin
    // ---------------------------------------------------------

    override suspend fun toggleFavorite(symbol: String) {
        dao.toggleFavorite(symbol)
    }

    override suspend fun pinCoin(symbol: String) {
        dao.clearPinned()
        dao.pinCoin(symbol)
    }

    override suspend fun unPinCoin() {
        dao.clearPinned()
    }

    override suspend fun getPinnedCoin(): Coin? {
        return dao.getPinnedCoin()?.toDomain()
    }
}