package com.gurmeet.coindevta.presentation.chart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.gurmeet.coindevta.analytics.AnalyticsConstants
import com.gurmeet.coindevta.analytics.AnalyticsEvent
import com.gurmeet.coindevta.analytics.AnalyticsLogger
import com.gurmeet.coindevta.ui.theme.CoinDevtaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChartActivity : ComponentActivity() {

    private val viewModel: ChartViewModel by viewModels()

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent()

        val chartScreenUi = ChartScreenUi()

        setContent {

            val state by viewModel.uiState.collectAsState()

            var isFoldExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    WindowInfoTracker.getOrCreate(this@ChartActivity)
                        .windowLayoutInfo(this@ChartActivity)
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

            LaunchedEffect(viewModel.currentSymbol) {
                viewModel.initialize()
            }

            CoinDevtaTheme {
                chartScreenUi.LayoutUI(
                    symbol = viewModel.currentSymbol,
                    state = state,
                    isFoldExpanded = isFoldExpanded,
                    onEvent = { event ->
                        when (event) {
                            is ChartUiEvent.OnIntervalSelected ->
                                viewModel.loadChart(
                                    viewModel.currentSymbol,
                                    event.interval
                                )

                            ChartUiEvent.OnBackClicked -> {
                                analyticsLogger.track(
                                    AnalyticsEvent(
                                        AnalyticsConstants.Chart.BACK_CLICKED,
                                        mapOf(
                                            AnalyticsConstants.Chart.PARAM_SYMBOL to viewModel.currentSymbol
                                        )
                                    )
                                )
                                finish()
                            }
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