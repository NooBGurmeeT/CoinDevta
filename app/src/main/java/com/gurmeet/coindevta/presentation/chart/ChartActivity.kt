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

        setContent {

            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.loadChart(symbol)
            }

            CoinDevtaTheme {
                ChartScreen(
                    symbol = symbol,
                    uiState = uiState,
                    livePrice = viewModel.livePrice.value,
                    onBack = { finish() }
                )
            }
        }
    }
}