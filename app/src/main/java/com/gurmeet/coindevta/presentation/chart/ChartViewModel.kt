package com.gurmeet.coindevta.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gurmeet.coindevta.analytics.AnalyticsConstants
import com.gurmeet.coindevta.analytics.AnalyticsEvent
import com.gurmeet.coindevta.analytics.AnalyticsLogger
import com.gurmeet.coindevta.domain.usecase.GetChartUseCase
import com.gurmeet.coindevta.domain.usecase.ObserveLivePricesUseCase
import com.gurmeet.coindevta.logger.ErrorLogger
import com.gurmeet.coindevta.logger.LogLevel
import com.gurmeet.coindevta.presentation.home.HomeEffect
import com.gurmeet.coindevta.util.NetworkMonitor
import com.gurmeet.coindevta.util.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val getChartUseCase: GetChartUseCase,
    private val observeLivePricesUseCase: ObserveLivePricesUseCase,
    private val analyticsLogger: AnalyticsLogger,
    private val errorLogger: ErrorLogger,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    companion object {
        private const val TAG = "ChartViewModel"
    }

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState

    private val _effect = MutableSharedFlow<ChartEffect>()
    val effect = _effect.asSharedFlow()

    var currentSymbol: String = ""
    private var initialPrice: Double = 0.0
    private var initialIsPositive: Boolean = true

    private var latestLivePrice: Double? = null
    private var chartJob: Job? = null


    init {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->

                if (!connected) {
                    _effect.emit(
                        ChartEffect.ShowToast("No internet connection")
                    )
                } else {
                    initialize()
                }
            }
        }
    }
    /**
     * Sets initial data passed from previous screen.
     */
    fun setInitialData(
        symbol: String,
        latestPrice: Double,
        isPositive: Boolean
    ) {
        currentSymbol = symbol
        initialPrice = latestPrice
        initialIsPositive = isPositive

        _uiState.update {
            it.copy(
                livePrice = latestPrice,
                isPositive24h = isPositive
            )
        }
    }

    /**
     * Initializes chart loading and live price observation.
     */
    fun initialize() {
        if (currentSymbol.isBlank()) return

        analyticsLogger.track(
            AnalyticsEvent(
                AnalyticsConstants.Chart.SCREEN_OPENED,
                mapOf(AnalyticsConstants.Chart.PARAM_SYMBOL to currentSymbol)
            )
        )

        loadChart(currentSymbol, ChartInterval.HOUR)
        observeLivePrices()
    }

    fun loadChart(symbol: String, interval: ChartInterval) {

        chartJob?.cancel()

        analyticsLogger.track(
            AnalyticsEvent(
                AnalyticsConstants.Chart.INTERVAL_SELECTED,
                mapOf(
                    AnalyticsConstants.Chart.PARAM_SYMBOL to symbol,
                    AnalyticsConstants.Chart.PARAM_INTERVAL to interval.name
                )
            )
        )

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedInterval = interval,
                    error = null
                )
            }

            when (val response = getChartUseCase(
                symbol,
                interval.apiValue,
                interval.limit
            )) {

                is Response.Success -> {

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chartPoints = response.data
                        )
                    }

                    startLiveAppendLoop()
                }

                is Response.Error -> {

                    errorLogger.log(
                        tag = TAG,
                        message = "Chart load failed for $symbol",
                        level = LogLevel.ERROR,
                        throwable = response.throwable
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.message
                        )
                    }

                    _effect.emit(
                        ChartEffect.ShowToast(
                            response.message ?: "Failed to load chart data"
                        )
                    )
                }

                else -> Unit
            }
        }
    }

    private fun observeLivePrices() {

        viewModelScope.launch {

            try {
                observeLivePricesUseCase().collect { update ->

                    if (update.symbol == currentSymbol) {

                        latestLivePrice = update.currentPrice

                        _uiState.update {
                            it.copy(
                                livePrice = update.currentPrice,
                                isPositive24h = update.isPositive24h
                            )
                        }
                    }
                }
            } catch (e: Exception) {

                errorLogger.log(
                    TAG,
                    "Live price observer failed",
                    LogLevel.ERROR,
                    e
                )

                _effect.emit(
                    ChartEffect.ShowToast("Live price connection lost")
                )
            }
        }
    }

    /**
     * Periodically appends latest live price to chart.
     */
    private fun startLiveAppendLoop() {

        chartJob = viewModelScope.launch {

            while (isActive) {

                delay(_uiState.value.selectedInterval.updateMillis)

                val price = latestLivePrice ?: continue

                _uiState.update { state ->

                    val updated = state.chartPoints
                        .toMutableList()
                        .apply {

                            add(
                                ChartPoint(
                                    time = System.currentTimeMillis(),
                                    price = price
                                )
                            )

                            if (size > state.selectedInterval.limit) {
                                removeAt(0)
                            }
                        }

                    state.copy(chartPoints = updated)
                }
            }
        }
    }
}