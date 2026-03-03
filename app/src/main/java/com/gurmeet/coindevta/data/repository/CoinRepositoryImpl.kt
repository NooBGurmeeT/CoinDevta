package com.gurmeet.coindevta.data.repository

import com.gurmeet.coindevta.data.local.dao.CoinDao
import com.gurmeet.coindevta.data.mapper.toDomain
import com.gurmeet.coindevta.data.mapper.toEntity
import com.gurmeet.coindevta.data.remote.api.BinanceApi
import com.gurmeet.coindevta.data.remote.websocket.BinanceSocketManager
import com.gurmeet.coindevta.domain.model.Coin
import com.gurmeet.coindevta.domain.model.TickerUpdate
import com.gurmeet.coindevta.domain.repository.CoinRepository
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.util.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Repository that coordinates REST API, WebSocket stream,
 * and local database for coin data.
 */
class CoinRepositoryImpl @Inject constructor(
    private val api: BinanceApi,
    private val dao: CoinDao,
    private val socketManager: BinanceSocketManager,
    private val errorLogger: ErrorLogger
) : CoinRepository {

    companion object {
        private const val TAG = "CoinRepositoryImpl"
    }

    /**
     * Fetches coins from API, merges with existing DB flags,
     * and stores them locally.
     */
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

            errorLogger.log(
                tag = TAG,
                message = "Initial data load failed",
                level = LogLevel.ERROR,
                throwable = e
            )

            Response.Error("Initial load failed", e)
        }
    }

    /**
     * Observes coin list from database and maps to domain model.
     */
    override fun observeCoins(): Flow<List<Coin>> {
        return dao.observeCoins()
            .map { list -> list.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Streams live ticker updates without writing to database.
     */
    override fun observeLivePrices(): Flow<TickerUpdate> {
        return socketManager.observeAllPrices()
            .buffer(capacity = 5000)
            .conflate()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Periodically refreshes static market data via REST API.
     */
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

            errorLogger.log(
                tag = TAG,
                message = "Price sync failed",
                level = LogLevel.ERROR,
                throwable = e
            )

            Response.Error("Sync failed", e)
        }
    }

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