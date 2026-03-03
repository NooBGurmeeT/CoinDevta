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
import androidx.compose.ui.tooling.preview.Preview
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
            containerColor = Color(0xFFF8FAFC),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            symbol.replace("USDT", ""),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { onEvent(ChartUiEvent.OnBackClicked) }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->

            if (!useTwoPaneLayout) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    PriceHeaderCard(symbol, state.livePrice, state.isPositive24h)

                    Text(
                        text = "Select Duration",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF475569)
                    )

                    IntervalSelector(
                        selected = state.selectedInterval,
                        onSelected = {
                            onEvent(ChartUiEvent.OnIntervalSelected(it))
                        }
                    )

                    Text(
                        text = "Live Chart",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    ChartContent(state)
                }

            } else {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        PriceHeaderCard(symbol, state.livePrice, state.isPositive24h)

                        Text(
                            text = "Select Duration",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF475569)
                        )

                        IntervalSelector(
                            selected = state.selectedInterval,
                            onSelected = {
                                onEvent(ChartUiEvent.OnIntervalSelected(it))
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Text(
                            text = "Live Chart",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Text(
                    state.error,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                ChartCard(
                    points = state.chartPoints,
                    interval = state.selectedInterval
                )
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
            if (isPositive24h) Color(0xFF16A34A)
            else Color(0xFFDC2626)

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {

                Text(
                    text = symbol.replace("USDT", ""),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "$ %.4f".format(price),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            ChartInterval.values().forEach { interval ->

                val selectedColor =
                    if (interval == selected)
                        Color(0xFF16A34A)
                    else
                        Color(0xFFE2E8F0)

                val textColor =
                    if (interval == selected)
                        Color.White
                    else
                        Color(0xFF475569)

                Button(
                    onClick = { onSelected(interval) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColor
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        interval.name,
                        color = textColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun ChartCard(
        points: List<ChartPoint>,
        interval: ChartInterval
    ) {

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                LineChart(points, interval)
            }
        }
    }

    @Composable
    private fun LineChart(
        points: List<ChartPoint>,
        interval: ChartInterval
    ) {

        if (points.size < 2) {
            Text("No chart data")
            return
        }

        val configuration = LocalConfiguration.current
        val graphHeight = configuration.screenHeightDp.dp * 0.35f

        val prices = points.map { it.price }
        val max = prices.maxOrNull() ?: return
        val min = prices.minOrNull() ?: return
        val range = (max - min).takeIf { it != 0.0 } ?: 1.0

        Column(Modifier.fillMaxWidth()) {

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(graphHeight)
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
                    color = Color(0xFF16A34A),
                    style = Stroke(width = 5f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text("High", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("%.4f".format(max), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Low", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("%.4f".format(min), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Divider(color = Color(0xFFE2E8F0))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(format(points.first().time, interval), fontSize = 11.sp, color = Color(0xFF64748B))
                Text(format(points[points.size / 2].time, interval), fontSize = 11.sp, color = Color(0xFF64748B))
                Text(format(points.last().time, interval), fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }

    private fun format(
        time: Long,
        interval: ChartInterval
    ): String {

        val date = Date(time)

        return when (interval) {

            ChartInterval.HOUR,
            ChartInterval.DAY ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

            ChartInterval.MONTH ->
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)

            else ->
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChartContent() {
    val samplePoints = listOf(
        ChartPoint(System.currentTimeMillis() - 30000, 100.0),
        ChartPoint(System.currentTimeMillis() - 20000, 120.0),
        ChartPoint(System.currentTimeMillis() - 10000, 110.0),
        ChartPoint(System.currentTimeMillis(), 130.0)
    )

    ChartScreenUi().LayoutUI(
        symbol = "BTCUSDT",
        state = ChartUiState(
            chartPoints = samplePoints,
            livePrice = 130.0,
            isPositive24h = true
        ),
        isFoldExpanded = false,
        onEvent = {}
    )
}