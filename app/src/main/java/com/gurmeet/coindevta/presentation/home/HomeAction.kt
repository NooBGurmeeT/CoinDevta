package com.gurmeet.coindevta.presentation.home

sealed class HomeAction {

    data class ToggleFavorite(val symbol: String) : HomeAction()
    data class PinCoin(val symbol: String) : HomeAction()
    object UnPinCoin : HomeAction()

    object ToggleExpandFavorites : HomeAction()

    data class CoinClick(val symbol: String) : HomeAction()
    object BackClick : HomeAction()

    data class SearchChanged(val query: String) : HomeAction()

    object OpenFilter : HomeAction()
    object CloseFilter : HomeAction()
    data class SortSelected(val sortType: SortType) : HomeAction()

    data class FoldableChanged(val expanded: Boolean) : HomeAction()
}