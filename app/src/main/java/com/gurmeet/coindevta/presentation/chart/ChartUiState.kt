package com.gurmeet.coindevta.presentation.chart

data class ChartUiState(
    val isLoading: Boolean = false,
    val chartPrices: List<Double> = emptyList(),
    val livePrice: Double = 0.0,
    val isPositive24h: Boolean = true,
    val selectedInterval: ChartInterval = ChartInterval.HOUR,
    val error: String? = null
)