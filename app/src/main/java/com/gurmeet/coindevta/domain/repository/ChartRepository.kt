package com.gurmeet.coindevta.domain.repository

import com.gurmeet.coindevta.util.Response

interface ChartRepository {
    suspend fun getChart(
        symbol: String,
        interval: String,
        limit: Int
    ): Response<List<Double>>
}