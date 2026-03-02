package com.gurmeet.coindevta.presentation.chart

// presentation/chart/ChartUiState.kt
data class ChartUiState(
    val isLoading: Boolean = false,
    val chartPrices: List<Double> = emptyList(),
    val error: String? = null
)