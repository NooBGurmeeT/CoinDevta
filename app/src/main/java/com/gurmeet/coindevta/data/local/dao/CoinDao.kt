package com.gurmeet.coindevta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gurmeet.coindevta.data.local.entity.CoinEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface CoinDao {

    @Query("SELECT * FROM coins ORDER BY symbol")
    fun observeCoins(): Flow<List<CoinEntity>>

    @Query("SELECT * FROM coins")
    suspend fun getAllCoinsOnce(): List<CoinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coins: List<CoinEntity>)

    @Query("""
    UPDATE coins 
    SET price = :price,
        marketCap = :marketCap,
        changePercent24h = :changePercent24h
    WHERE symbol = :symbol
""")
    suspend fun updateCoinData(
        symbol: String,
        price: Double,
        marketCap: Double,
        changePercent24h: Double
    )

    @Query("UPDATE coins SET price = :price WHERE symbol = :symbol")
    suspend fun updatePrice(symbol: String, price: Double)

    @Query("UPDATE coins SET isFavorite = NOT isFavorite WHERE symbol = :symbol")
    suspend fun toggleFavorite(symbol: String)

    @Query("UPDATE coins SET isPinned = 0")
    suspend fun clearPinned()

    @Query("UPDATE coins SET isPinned = 1 WHERE symbol = :symbol")
    suspend fun pinCoin(symbol: String)

    @Query("SELECT * FROM coins WHERE isPinned = 1 LIMIT 1")
    suspend fun getPinnedCoin(): CoinEntity?
}