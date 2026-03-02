package com.gurmeet.coindevta.presentation.home

import com.gurmeet.coindevta.domain.model.Coin

data class HomeScreenState(
    val isLoading: Boolean = false,
    val isExpandedFavorites: Boolean = false,
    val isFoldableExpanded: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedSort: SortType = SortType.MARKET_CAP_DESC,
    val coins: List<Coin> = emptyList(),
    val priceMap: Map<String, Double> = emptyMap(),
    val error: String? = null
)