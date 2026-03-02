package com.gurmeet.coindevta.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gurmeet.coindevta.domain.usecase.*
import com.gurmeet.coindevta.util.Response
import com.gurmeet.coindevta.util.windowedByTime
import com.gurmeet.coindevta.widget.WidgetSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadInitialData: LoadInitialDataUseCase,
    private val observeCoinsUseCase: ObserveCoinsUseCase,
    private val observeLivePricesUseCase: ObserveLivePricesUseCase,
    private val syncPricesUseCase: SyncPricesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val pinCoinUseCase: PinCoinUseCase,
    private val unPinCoinUseCase: UnPinCoinUseCase,
    private val widgetSyncManager: WidgetSyncManager
) : ViewModel() {

    // 🔥 Live prices separated (NOT part of combine)
    private val _livePriceMap =
        MutableStateFlow<Map<String, Double>>(emptyMap())
    val livePriceMap: StateFlow<Map<String, Double>> = _livePriceMap

    private val searchQuery = MutableStateFlow("")
    private val sortType = MutableStateFlow(SortType.MARKET_CAP_DESC)
    private val uiFlags = MutableStateFlow(HomeScreenState())

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect = _effect.asSharedFlow()

    private val baseCoinsFlow =
        observeCoinsUseCase()
            .distinctUntilChanged()


    val state: StateFlow<HomeScreenState> =
        combine(
            baseCoinsFlow,
            searchQuery,
            sortType,
            uiFlags
        ) { coins, query, sort, flags ->

            val filtered =
                if (query.isBlank()) coins
                else coins.filter {
                    it.name.contains(query.trim(), true)
                }

            val sorted = when (sort) {
                SortType.MARKET_CAP_ASC -> filtered.sortedBy { it.marketCap }
                SortType.MARKET_CAP_DESC -> filtered.sortedByDescending { it.marketCap }
                SortType.CHANGE_ASC -> filtered.sortedBy { it.changePercent24h }
                SortType.CHANGE_DESC -> filtered.sortedByDescending { it.changePercent24h }
                SortType.NAME_ASC -> filtered.sortedBy { it.name }
                SortType.NAME_DESC -> filtered.sortedByDescending { it.name }
            }

            flags.copy(
                coins = sorted,
                searchQuery = query,
                selectedSort = sort
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeScreenState()
        )

    init {
        loadData()
        observeLivePrices()
        startPeriodicSync()
    }

    private fun loadData() {
        viewModelScope.launch {
            uiFlags.update { it.copy(isLoading = true) }

            when (val result = loadInitialData()) {
                is Response.Success ->
                    uiFlags.update { it.copy(isLoading = false) }

                is Response.Error ->
                    uiFlags.update {
                        it.copy(isLoading = false, error = result.message)
                    }

                else -> {}
            }
        }
    }

    private fun observeLivePrices() {

        viewModelScope.launch(Dispatchers.IO) {

            val socketUpdates =
                MutableSharedFlow<Pair<String, Double>>(
                    extraBufferCapacity = 5000,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
                )

            // 1️⃣ Collect raw socket continuously
            launch {
                observeLivePricesUseCase()
                    .collect { update ->
                        socketUpdates.tryEmit(update)
                    }
            }

            // 2️⃣ Batch every 1 second
            socketUpdates
                .buffer()               // avoid backpressure
                .windowedByTime(1000L)  // custom extension below
                .collect { batch ->

                    if (batch.isNotEmpty()) {

                        val latestPerSymbol =
                            batch.groupBy { it.first }
                                .mapValues { entry ->
                                    entry.value.last().second
                                }

                        _livePriceMap.update { current ->
                            current + latestPerSymbol
                        }
                    }
                }
        }
    }

    private fun startPeriodicSync() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(60_000)
                syncPricesUseCase()
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {

            is HomeAction.ToggleFavorite ->
                viewModelScope.launch {
                    toggleFavoriteUseCase(action.symbol)
                    widgetSyncManager.syncFavorites()
                }

            is HomeAction.PinCoin -> {
                viewModelScope.launch {
                    pinCoinUseCase(action.symbol)
                    _effect.emit(HomeEffect.StartPinnedService)
                }
            }

            HomeAction.UnPinCoin -> {
                viewModelScope.launch {
                    unPinCoinUseCase()
                    _effect.emit(HomeEffect.StopPinnedService)
                }
            }

            is HomeAction.SearchChanged ->
                searchQuery.value = action.query

            is HomeAction.SortSelected ->
                sortType.value = action.sortType

            HomeAction.OpenFilter ->
                uiFlags.update { it.copy(isBottomSheetOpen = true) }

            HomeAction.CloseFilter ->
                uiFlags.update { it.copy(isBottomSheetOpen = false) }

            HomeAction.ToggleExpandFavorites ->
                uiFlags.update {
                    it.copy(isExpandedFavorites = !it.isExpandedFavorites)
                }

            is HomeAction.CoinClick ->
                viewModelScope.launch {
                    _effect.emit(
                        HomeEffect.NavigateToChart(action.symbol)
                    )
                }

            HomeAction.BackClick ->
                viewModelScope.launch {
                    _effect.emit(HomeEffect.NavigateBack)
                }

            is HomeAction.FoldableChanged ->
                uiFlags.update { it.copy(isFoldableExpanded = action.expanded) }
        }
    }
}