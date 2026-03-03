package com.gurmeet.coindevta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.gurmeet.coindevta.analytics.AnalyticsConstants
import com.gurmeet.coindevta.analytics.AnalyticsEvent
import com.gurmeet.coindevta.analytics.AnalyticsLogger
import javax.inject.Inject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analyticsLogger.track(
            AnalyticsEvent(AnalyticsConstants.Main.ACTIVITY_OPENED)
        )
        setContent {

            val windowSizeClass = calculateWindowSizeClass(this)
            var isFoldExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    WindowInfoTracker.getOrCreate(this@MainActivity)
                        .windowLayoutInfo(this@MainActivity)
                        .collect { layoutInfo ->

                            val foldingFeature = layoutInfo.displayFeatures
                                .filterIsInstance<FoldingFeature>()
                                .firstOrNull()

                            isFoldExpanded =
                                foldingFeature != null &&
                                        foldingFeature.state == FoldingFeature.State.FLAT &&
                                        foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL
                        }
                }
            }

            val isExpanded =
                windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                        || isFoldExpanded

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


    private fun startWidgetService() {
        val intent = Intent(this, CoinWidgetService::class.java)
        startForegroundService(intent)
    }


    private fun startPinnedPriceService() {
        val intent = Intent(
            this@MainActivity,
            PinnedPriceService::class.java
        )
        startForegroundService(intent)
    }
}