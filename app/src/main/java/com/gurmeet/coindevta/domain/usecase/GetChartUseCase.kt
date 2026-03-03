package com.gurmeet.coindevta.domain.usecase

import com.gurmeet.coindevta.domain.repository.ChartRepository
import com.gurmeet.coindevta.presentation.chart.ChartPoint
import com.gurmeet.coindevta.util.Response
import javax.inject.Inject

class GetChartUseCase @Inject constructor(
    private val repository: ChartRepository
) {

    suspend operator fun invoke(
        symbol: String,
        interval: String,
        limit: Int
    ): Response<List<ChartPoint>>  {
        return repository.getChart(
            symbol = symbol,
            interval = interval,
            limit = limit
        )
    }
}