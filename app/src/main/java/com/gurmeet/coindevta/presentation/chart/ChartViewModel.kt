package com.gurmeet.coindevta.presentation.chart

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gurmeet.coindevta.domain.usecase.GetCurrentDayChartUseCase
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
    private val getCurrentDayChartUseCase: GetCurrentDayChartUseCase,
    private val observeLivePricesUseCase: ObserveLivePricesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState

    val livePrice = mutableStateOf(0.0)

    fun loadChart(symbol: String) {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            when (val response = getCurrentDayChartUseCase(symbol)) {

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

                else -> {}
            }
        }
    }
}