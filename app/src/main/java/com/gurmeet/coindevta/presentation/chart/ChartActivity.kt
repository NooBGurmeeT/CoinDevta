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

        val symbol = intent.getStringExtra("symbol") ?: ""

        val chartScreenUi = ChartScreenUi()

        setContent {

            val state by viewModel.uiState.collectAsState()

            LaunchedEffect(symbol) {
                viewModel.initialize(symbol)
            }

            CoinDevtaTheme {
                chartScreenUi.LayoutUI(
                    symbol = symbol,
                    state = state,
                    onEvent = { event ->
                        when (event) {
                            is ChartUiEvent.OnIntervalSelected ->
                                viewModel.loadChart(symbol, event.interval)

                            ChartUiEvent.OnBackClicked ->
                                finish()
                        }
                    }
                )
            }
        }
    }
}