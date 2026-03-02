package com.gurmeet.coindevta.presentation.chart

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gurmeet.coindevta.domain.usecase.GetChartUseCase
import com.gurmeet.coindevta.domain.usecase.ObserveLivePricesUseCase
import com.gurmeet.coindevta.util.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedInterval = interval,
                    error = null
                )
            }

            when (val response = getChartUseCase(
                symbol = symbol,
                interval = interval.apiValue,
                limit = interval.limit
            )) {

                is Response.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            chartPrices = response.data
                        )
                    }
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

                    _uiState.update {
                        it.copy(
                            livePrice = update.currentPrice,
                            isPositive24h = update.isPositive24h,
                        )
                    }
                }
            }
        }
    }
}