package com.gurmeet.coindevta.presentation.chart

sealed class ChartUiEvent {
    data class OnIntervalSelected(val interval: ChartInterval) : ChartUiEvent()
    object OnBackClicked : ChartUiEvent()
}