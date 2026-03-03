package com.gurmeet.coindevta.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gurmeet.coindevta.analytics.AnalyticsConstants
import com.gurmeet.coindevta.analytics.AnalyticsEvent
import com.gurmeet.coindevta.analytics.AnalyticsLogger
import com.gurmeet.coindevta.domain.model.TickerUpdate
import com.gurmeet.coindevta.domain.usecase.*
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.util.NetworkMonitor
import com.gurmeet.coindevta.util.Response
import com.gurmeet.coindevta.util.windowedByTime
import com.gurmeet.coindevta.widget.WidgetSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel responsible for managing Home screen state,
 * combining database data, websocket updates, sorting,
 * searching and handling all user actions.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadInitialData: LoadInitialDataUseCase,
    private val observeCoinsUseCase: ObserveCoinsUseCase,
    private val observeLivePricesUseCase: ObserveLivePricesUseCase,
    private val syncPricesUseCase: SyncPricesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val pinCoinUseCase: PinCoinUseCase,
    private val unPinCoinUseCase: UnPinCoinUseCase,
    private val widgetSyncManager: WidgetSyncManager,
    private val analyticsLogger: AnalyticsLogger,
    private val errorLogger: ErrorLogger,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val searchQuery = MutableStateFlow("")
    private val sortType = MutableStateFlow(SortType.MARKET_CAP_DESC)
    private val uiFlags = MutableStateFlow(HomeScreenState())

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect = _effect.asSharedFlow()

    private val baseCoinsFlow =
        observeCoinsUseCase().distinctUntilChanged()

    /**
     * Public UI state combining:
     * - Database coin list
     * - Search query
     * - Sorting type
     * - UI flags (loading, bottom sheet, etc.)
     */
    val state: StateFlow<HomeScreenState> =
        combine(baseCoinsFlow, searchQuery, sortType, uiFlags)
        { coins, query, sort, flags ->

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

        analyticsLogger.track(
            AnalyticsEvent(AnalyticsConstants.Home.SCREEN_OPENED)
        )

        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->

                if (!connected) {
                    _effect.emit(
                        HomeEffect.ShowToast("No internet connection")
                    )
                }
            }
        }
    }

    /**
     * Loads initial coin data from remote API
     * and updates loading and error states accordingly.
     */
    private fun loadData() {
        viewModelScope.launch {

            uiFlags.update { it.copy(isLoading = true) }

            when (val result = loadInitialData()) {

                is Response.Success ->
                    uiFlags.update { it.copy(isLoading = false) }

                is Response.Error -> {

                    errorLogger.log(
                        TAG,
                        "Initial load failed",
                        LogLevel.ERROR,
                        result.throwable
                    )

                    uiFlags.update {
                        it.copy(isLoading = false, error = result.message)
                    }

                    _effect.emit(
                        HomeEffect.ShowToast(result.message ?: "Something went wrong")
                    )
                }

                else -> {}
            }
        }
    }

    /**
     * Collects websocket price updates,
     * batches them every 1 second,
     * and updates only the latest value per symbol.
     */
    @OptIn(FlowPreview::class)
    private fun observeLivePrices() {

        viewModelScope.launch(Dispatchers.IO) {

            val socketUpdates =
                MutableSharedFlow<TickerUpdate>(
                    extraBufferCapacity = 5000,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
                )

            launch {
                observeLivePricesUseCase()
                    .collect { update ->
                        socketUpdates.tryEmit(update)
                    }
            }

            socketUpdates
                .buffer()
                .windowedByTime(1000L)
                .collect { batch ->

                    if (batch.isNotEmpty()) {

                        val latestPerSymbol =
                            batch.groupBy { it.symbol }
                                .mapValues { entry ->
                                    entry.value.last()
                                }

                        uiFlags.update { currentState ->
                            currentState.copy(
                                tickerMap =
                                    currentState.tickerMap + latestPerSymbol
                            )
                        }
                    }
                }
        }
    }

    /**
     * Performs periodic REST sync every 60 seconds
     * to update static market data.
     */
    private fun startPeriodicSync() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(60_000)

                try {
                    syncPricesUseCase()
                } catch (e: Exception) {
                    errorLogger.log(
                        TAG,
                        "Periodic sync failed",
                        LogLevel.ERROR,
                        e
                    )
                    _effect.emit(
                        HomeEffect.ShowToast("Failed to sync latest prices")
                    )
                }
            }
        }
    }

    /**
     * Handles all user actions from UI.
     *
     * @param action Represents user interaction such as
     * toggling favorite, pinning coin, searching, sorting,
     * navigation and foldable state changes.
     */
    fun onAction(action: HomeAction) {

        when (action) {

            is HomeAction.ToggleFavorite ->
                viewModelScope.launch {
                    toggleFavoriteUseCase(action.symbol)
                    widgetSyncManager.syncFavorites()

                    analyticsLogger.track(
                        AnalyticsEvent(
                            AnalyticsConstants.Home.FAVORITE_TOGGLED,
                            mapOf(
                                AnalyticsConstants.Home.PARAM_SYMBOL to action.symbol
                            )
                        )
                    )
                }

            is HomeAction.PinCoin ->
                viewModelScope.launch {
                    pinCoinUseCase(action.symbol)
                    _effect.emit(HomeEffect.StartPinnedService)

                    analyticsLogger.track(
                        AnalyticsEvent(
                            AnalyticsConstants.Home.COIN_PINNED,
                            mapOf(
                                AnalyticsConstants.Home.PARAM_SYMBOL to action.symbol
                            )
                        )
                    )
                }

            HomeAction.UnPinCoin ->
                viewModelScope.launch {
                    unPinCoinUseCase()
                    _effect.emit(HomeEffect.StopPinnedService)

                    analyticsLogger.track(
                        AnalyticsEvent(
                            AnalyticsConstants.Home.COIN_UNPINNED
                        )
                    )
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
                    it.copy(
                        isExpandedFavorites =
                            !it.isExpandedFavorites
                    )
                }

            is HomeAction.CoinClick ->
                viewModelScope.launch {

                    analyticsLogger.track(
                        AnalyticsEvent(
                            AnalyticsConstants.Home.COIN_CLICKED,
                            mapOf(
                                AnalyticsConstants.Home.PARAM_SYMBOL to action.symbol
                            )
                        )
                    )

                    _effect.emit(
                        HomeEffect.NavigateToChart(action.symbol)
                    )
                }

            HomeAction.BackClick ->
                viewModelScope.launch {
                    _effect.emit(HomeEffect.NavigateBack)
                }

            is HomeAction.FoldableChanged ->
                uiFlags.update {
                    it.copy(
                        isFoldableExpanded = action.expanded
                    )
                }
        }
    }
}