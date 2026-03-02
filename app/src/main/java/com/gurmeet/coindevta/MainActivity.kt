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
            val livePrices by viewModel.livePriceMap.collectAsState()

            CoinDevtaTheme {
                HomeScreenUi().LayUi(
                    state = state,
                    livePrices = livePrices,
                    onAction = viewModel::onAction
                )
            }
        }

        observeEffects()
        startWidgetService()
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect) {

                    is HomeEffect.NavigateToChart -> {
                        val intent =
                            Intent(this@MainActivity, ChartActivity::class.java)
                        intent.putExtra("symbol", effect.symbol)
                        startActivity(intent)
                    }

                    HomeEffect.NavigateBack -> finish()
                    HomeEffect.StartPinnedService -> {
                        val intent = Intent(
                            this@MainActivity,
                            PinnedPriceService::class.java
                        )
                        startForegroundService(intent)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startWidgetService() {
        val intent = Intent(this, CoinWidgetService::class.java)
        startForegroundService(intent)
    }
}