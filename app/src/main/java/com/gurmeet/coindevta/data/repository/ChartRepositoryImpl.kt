package com.gurmeet.coindevta.data.repository

import com.gurmeet.coindevta.data.remote.api.BinanceApi
import com.gurmeet.coindevta.domain.repository.ChartRepository
import com.gurmeet.coindevta.presentation.chart.ChartPoint
import com.gurmeet.coindevta.util.Response
import javax.inject.Inject

class ChartRepositoryImpl @Inject constructor(
    private val api: BinanceApi
) : ChartRepository {
    override suspend fun getChart(
        symbol: String,
        interval: String,
        limit: Int
    ): Response<List<ChartPoint>> {

        return try {

            val klines = api.getKlines(symbol, interval, limit)

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
            e.printStackTrace()
            Response.Error(e.message ?: "Chart fetch failed", e)
        }
    }
}