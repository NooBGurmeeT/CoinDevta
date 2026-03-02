package com.gurmeet.coindevta.presentation.home

import com.gurmeet.coindevta.domain.model.Coin
import com.gurmeet.coindevta.domain.model.TickerUpdate

data class HomeScreenState(
    val isLoading: Boolean = false,
    val isExpandedFavorites: Boolean = false,
    val isFoldableExpanded: Boolean = false,
    val isBottomSheetOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedSort: SortType = SortType.MARKET_CAP_DESC,
    val coins: List<Coin> = emptyList(),
    val tickerMap: Map<String, TickerUpdate> = emptyMap(),
    val error: String? = null
)