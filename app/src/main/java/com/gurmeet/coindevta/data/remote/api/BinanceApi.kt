package com.gurmeet.coindevta.data.remote.api

import com.gurmeet.coindevta.data.remote.dto.CoinDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApi {

    @GET("api/v3/ticker/24hr")
    suspend fun getAllCoins(): List<CoinDto>

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1m",
        @Query("limit") limit: Int = 1440
    ): List<List<Any>>
}