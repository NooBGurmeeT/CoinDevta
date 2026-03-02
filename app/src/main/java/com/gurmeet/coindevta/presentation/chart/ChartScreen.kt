package com.gurmeet.coindevta.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ChartScreenUi {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LayoutUI(
        symbol: String,
        state: ChartUiState,
        onEvent: (ChartUiEvent) -> Unit
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = symbol.replace("USDT", ""),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                onEvent(ChartUiEvent.OnBackClicked)
                            }
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            containerColor = Color(0xFFF5F7FA)
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {

                PriceHeaderCard(symbol, state.livePrice)

                Spacer(modifier = Modifier.height(20.dp))

                IntervalSelector(
                    selected = state.selectedInterval,
                    onSelected = {
                        onEvent(
                            ChartUiEvent.OnIntervalSelected(it)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.error != null -> {
                        Text(
                            text = state.error,
                            color = Color.Red
                        )
                    }

                    else -> {
                        ChartCard(state.chartPrices)
                    }
                }
            }
        }
    }

    // -------------------------------
    // Price Header
    // -------------------------------

    @Composable
    private fun PriceHeaderCard(
        symbol: String,
        price: Double
    ) {

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = symbol.replace("USDT", ""),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "₹ %.2f".format(price),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (price > 0)
                        Color(0xFF2ECC71)
                    else
                        Color.Red
                )
            }
        }
    }

    // -------------------------------
    // Interval Selector
    // -------------------------------

    @Composable
    private fun IntervalSelector(
        selected: ChartInterval,
        onSelected: (ChartInterval) -> Unit
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            ChartInterval.values().forEach { interval ->

                val isSelected = interval == selected

                Button(
                    onClick = { onSelected(interval) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (isSelected)
                                Color(0xFF2ECC71)
                            else
                                Color.LightGray
                    )
                ) {
                    Text(
                        text = interval.name,
                        color = if (isSelected)
                            Color.White
                        else
                            Color.Black
                    )
                }
            }
        }
    }

    // -------------------------------
    // Chart Card
    // -------------------------------

    @Composable
    private fun ChartCard(
        prices: List<Double>
    ) {

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .height(320.dp)
                    .padding(16.dp)
            ) {
                LineChart(prices)
            }
        }
    }

    // -------------------------------
    // Line Chart With Axis
    // -------------------------------

    @Composable
    private fun LineChart(prices: List<Double>) {

        if (prices.size < 2) {
            Text("No chart data")
            return
        }

        val maxPrice = prices.maxOrNull() ?: return
        val minPrice = prices.minOrNull() ?: return
        val range = (maxPrice - minPrice).takeIf { it != 0.0 } ?: 1.0

        Column {

            // -----------------
            // Chart Canvas
            // -----------------

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {

                val stepX = size.width / (prices.size - 1)

                val path = Path()

                prices.forEachIndexed { index, price ->

                    val x = index * stepX

                    val normalized = (price - minPrice) / range
                    val y = size.height - (normalized * size.height)

                    if (index == 0) {
                        path.moveTo(x, y.toFloat())
                    } else {
                        path.lineTo(x, y.toFloat())
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF2ECC71),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -----------------
            // Y Axis Values
            // -----------------

            Column {
                Text(
                    text = "High: %.2f".format(maxPrice),
                    fontSize = 12.sp
                )

                Text(
                    text = "Low: %.2f".format(minPrice),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -----------------
            // X Axis Values
            // -----------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start", fontSize = 12.sp)
                Text("Now", fontSize = 12.sp)
            }
        }
    }
}