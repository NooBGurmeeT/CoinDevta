package com.gurmeet.coindevta

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.gurmeet.coindevta.presentation.chart.ChartActivity
import com.gurmeet.coindevta.presentation.home.*
import com.gurmeet.coindevta.service.PinnedPriceService
import com.gurmeet.coindevta.ui.theme.CoinDevtaTheme
import com.gurmeet.coindevta.widget.CoinWidgetService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpanded =
                windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            LaunchedEffect(isExpanded) {
                viewModel.onAction(
                    HomeAction.FoldableChanged(isExpanded)
                )
            }

            val state by viewModel.state.collectAsState()

            CoinDevtaTheme {
                HomeScreenUi().LayUi(
                    state = state,
                    onAction = viewModel::onAction
                )
            }
        }

        observeEffects()
        startWidgetService()
        startPinnedPriceService()
    }

    private fun observeEffects() {
        lifecycleScope.launch {

            viewModel.effect.collect { effect ->

                when (effect) {

                    is HomeEffect.NavigateToChart -> {
                        startChartActivity(effect)
                    }

                    HomeEffect.NavigateBack -> finish()

                    HomeEffect.StartPinnedService -> {
                        startPinnedPriceService()
                    }

                    HomeEffect.StopPinnedService -> {
                        val intent = Intent(
                            this@MainActivity,
                            PinnedPriceService::class.java
                        )
                        stopService(intent)
                    }
                }
            }
        }
    }

    private fun startChartActivity(effect: HomeEffect.NavigateToChart) {
        val currentState = viewModel.state.value
        val ticker =
            currentState.tickerMap[effect.symbol]

        val latestPrice =
            ticker?.currentPrice ?: 0.0

        val isPositive =
            ticker?.isPositive24h ?: true

        val intent = Intent(
            this@MainActivity,
            ChartActivity::class.java
        ).apply {

            putExtra("symbol", effect.symbol)
            putExtra("latest_price", latestPrice)
            putExtra("is_positive", isPositive)
        }

        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startWidgetService() {
        val intent = Intent(this, CoinWidgetService::class.java)
        startForegroundService(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPinnedPriceService(){
        val intent = Intent(
            this@MainActivity,
            PinnedPriceService::class.java
        )
        startForegroundService(intent)
    }
}