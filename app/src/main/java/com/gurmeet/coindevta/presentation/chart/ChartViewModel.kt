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

    private var currentSymbol: String? = null

    // Called once from Activity
    fun initialize(symbol: String) {
        if (currentSymbol != null) return
        currentSymbol = symbol

        loadChart(symbol, ChartInterval.HOUR)
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

            observeLivePricesUseCase().collect { (symbol, price) ->

                if (symbol == currentSymbol) {

                    _uiState.update {
                        it.copy(livePrice = price)
                    }
                }
            }
        }
    }
}