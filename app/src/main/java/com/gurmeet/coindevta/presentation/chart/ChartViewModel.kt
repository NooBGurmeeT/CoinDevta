package com.gurmeet.coindevta.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gurmeet.coindevta.domain.usecase.GetChartUseCase
import com.gurmeet.coindevta.domain.usecase.ObserveLivePricesUseCase
import com.gurmeet.coindevta.util.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val getChartUseCase: GetChartUseCase,
    private val observeLivePricesUseCase: ObserveLivePricesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState

    var currentSymbol: String = ""
    private var initialPrice: Double = 0.0
    private var initialIsPositive: Boolean = true

    private var latestLivePrice: Double? = null
    private var chartJob: Job? = null

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

    fun initialize() {
        if (currentSymbol.isBlank()) return
        loadChart(currentSymbol, ChartInterval.HOUR)
        observeLivePrices()
    }

    fun loadChart(symbol: String, interval: ChartInterval) {

        chartJob?.cancel()

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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.message
                        )
                    }
                }

                else -> Unit
            }
        }
    }

    private fun observeLivePrices() {

        viewModelScope.launch {

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
        }
    }

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