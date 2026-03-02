package com.gurmeet.coindevta.data.repository

import com.gurmeet.coindevta.data.remote.api.BinanceApi
import com.gurmeet.coindevta.domain.repository.ChartRepository
import com.gurmeet.coindevta.util.Response
import javax.inject.Inject

class ChartRepositoryImpl @Inject constructor(
    private val api: BinanceApi
) : ChartRepository {
    override suspend fun getChart(
        symbol: String,
        interval: String,
        limit: Int
    ): Response<List<Double>> {
        return try {
            val klines = api.getKlines(symbol, interval, limit)
            val prices = klines.map { it[4].toString().toDouble() }
            Response.Success(prices)
        } catch (e: Exception) {
            Response.Error("Chart fetch failed", e)
        }
    }
}