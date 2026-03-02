package com.gurmeet.coindevta.presentation.chart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gurmeet.coindevta.ui.theme.CoinDevtaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChartActivity : ComponentActivity() {

    private val viewModel: ChartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent()

        val chartScreenUi = ChartScreenUi()

        setContent {

            val state by viewModel.uiState.collectAsState()

            LaunchedEffect(viewModel.currentSymbol) {
                viewModel.initialize()
            }

            CoinDevtaTheme {
                chartScreenUi.LayoutUI(
                    symbol = viewModel.currentSymbol,
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            is ChartUiEvent.OnIntervalSelected ->
                                viewModel.loadChart(viewModel.currentSymbol, event.interval)

                            ChartUiEvent.OnBackClicked ->
                                finish()
                        }
                    }
                )
            }
        }
    }

    private fun handleIntent() {
        val symbol = intent.getStringExtra("symbol") ?: ""
        val latestPrice =
            intent.getDoubleExtra("latest_price", 0.0)
        val isPositive =
            intent.getBooleanExtra("is_positive", true)

        viewModel.setInitialData(
            symbol,
            latestPrice,
            isPositive
        )
    }
}