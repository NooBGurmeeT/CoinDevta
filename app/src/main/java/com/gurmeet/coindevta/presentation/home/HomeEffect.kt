package com.gurmeet.coindevta.presentation.home

sealed class HomeEffect {

    object NavigateBack : HomeEffect()

    data class NavigateToChart(val symbol: String) : HomeEffect()

    object StartPinnedService : HomeEffect()

    object StopPinnedService : HomeEffect()
    data class ShowToast(val message: String) : HomeEffect()
}