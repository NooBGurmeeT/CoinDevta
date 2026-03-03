package com.gurmeet.coindevta.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

class ChartScreenUi {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LayoutUI(
        symbol: String,
        state: ChartUiState,
        isFoldExpanded: Boolean,
        onEvent: (ChartUiEvent) -> Unit
    ) {

        val configuration = LocalConfiguration.current
        val isTabletWidth = configuration.screenWidthDp >= 840
        val useTwoPaneLayout = isTabletWidth || isFoldExpanded

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            symbol.replace("USDT", ""),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { onEvent(ChartUiEvent.OnBackClicked) }
                        ) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    }
                )
            }
        ) { padding ->

            if (!useTwoPaneLayout) {

                // 📱 PHONE — FULL SCREEN SCROLL
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {

                    PriceHeaderCard(symbol, state.livePrice, state.isPositive24h)

                    Spacer(modifier = Modifier.height(20.dp))

                    IntervalSelector(
                        selected = state.selectedInterval,
                        onSelected = {
                            onEvent(ChartUiEvent.OnIntervalSelected(it))
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ChartContent(state)
                }

            } else {

                // 📖 TABLET / FOLDABLE OPEN
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {

                    // LEFT SIDE SCROLLABLE
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .verticalScroll(rememberScrollState())
                    ) {

                        PriceHeaderCard(symbol, state.livePrice, state.isPositive24h)

                        Spacer(modifier = Modifier.height(20.dp))

                        IntervalSelector(
                            selected = state.selectedInterval,
                            onSelected = {
                                onEvent(ChartUiEvent.OnIntervalSelected(it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // RIGHT SIDE SCROLLABLE
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ChartContent(state)
                    }
                }
            }
        }
    }

    @Composable
    private fun ChartContent(state: ChartUiState) {

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Text(state.error, color = Color.Red)
            }

            else -> {
                ChartCard(state.chartPoints)
            }
        }
    }

    @Composable
    private fun PriceHeaderCard(
        symbol: String,
        price: Double,
        isPositive24h: Boolean
    ) {

        val color =
            if (isPositive24h) Color(0xFF22C55E)
            else Color(0xFFEF4444)

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {

                Text(
                    text = symbol.replace("USDT", ""),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$ %.4f".format(price),
                    fontSize = 26.sp,
                    color = color
                )
            }
        }
    }

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

                val selectedColor =
                    if (interval == selected) Color(0xFF2ECC71)
                    else Color.LightGray

                Button(
                    onClick = { onSelected(interval) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColor
                    )
                ) {
                    Text(interval.name)
                }
            }
        }
    }

    @Composable
    private fun ChartCard(points: List<ChartPoint>) {

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LineChart(points)
            }
        }
    }
    @Composable
    private fun LineChart(points: List<ChartPoint>) {

        if (points.size < 2) {
            Text("No chart data")
            return
        }

        val prices = points.map { it.price }
        val max = prices.maxOrNull() ?: return
        val min = prices.minOrNull() ?: return
        val range = (max - min).takeIf { it != 0.0 } ?: 1.0

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {

                val stepX = size.width / (points.size - 1)
                val path = Path()

                points.forEachIndexed { index, point ->

                    val x = index * stepX
                    val y = size.height -
                            (((point.price - min) / range) * size.height)

                    if (index == 0) path.moveTo(x, y.toFloat())
                    else path.lineTo(x, y.toFloat())
                }

                drawPath(
                    path = path,
                    color = Color(0xFF2ECC71),
                    style = Stroke(width = 4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("High: %.4f".format(max), fontSize = 12.sp)
            Text("Low: %.4f".format(min), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(format(points.first().time), fontSize = 12.sp)
                Text(format(points[points.size / 2].time), fontSize = 12.sp)
                Text(format(points.last().time), fontSize = 12.sp)
            }
        }
    }

    private fun format(time: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(time))
    }
}