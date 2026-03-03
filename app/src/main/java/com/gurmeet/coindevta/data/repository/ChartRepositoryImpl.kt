package com.gurmeet.coindevta.data.repository

import com.gurmeet.coindevta.data.remote.api.BinanceApi
import com.gurmeet.coindevta.domain.repository.ChartRepository
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.presentation.chart.ChartPoint
import com.gurmeet.coindevta.util.Response
import javax.inject.Inject

/**
 * Repository implementation responsible for fetching and mapping
 * Binance kline data into ChartPoint domain model.
 */
class ChartRepositoryImpl @Inject constructor(
    private val api: BinanceApi,
    private val errorLogger: ErrorLogger
) : ChartRepository {

    companion object {
        private const val TAG = "ChartRepositoryImpl"
    }

    /**
     * Fetches historical chart data and maps raw kline response
     * into strongly typed ChartPoint list.
     */
    override suspend fun getChart(
        symbol: String,
        interval: String,
        limit: Int
    ): Response<List<ChartPoint>> {

        return try {

            val klines = api.getKlines(symbol, interval, limit)

            // Maps raw API response into ChartPoint model
            val points = klines.map {

                val time = when (val value = it[0]) {
                    is Double -> value.toLong()
                    is Long -> value
                    is String -> value.toDouble().toLong()
                    else -> 0L
                }

                val price = when (val value = it[4]) {
                    is Double -> value
                    is String -> value.toDouble()
                    else -> 0.0
                }

                ChartPoint(
                    time = time,
                    price = price
                )
            }

            Response.Success(points)

        } catch (e: Exception) {

            errorLogger.log(
                tag = TAG,
                message = "Failed to fetch chart data for $symbol",
                level = LogLevel.ERROR,
                throwable = e
            )

            Response.Error(e.message ?: "Chart fetch failed", e)
        }
    }
}